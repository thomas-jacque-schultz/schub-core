package schultz.thomas.schub.core.team.business.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.team.api.dto.ChampionPoolColumnDto;
import schultz.thomas.schub.core.team.api.dto.ChampionPoolDto;
import schultz.thomas.schub.core.team.api.dto.ChampionPoolEntryDto;
import schultz.thomas.schub.core.team.api.dto.ChampionPoolMemberDto;
import schultz.thomas.schub.core.team.business.model.GameRole;
import schultz.thomas.schub.core.team.business.model.MemberStatus;
import schultz.thomas.schub.core.team.business.model.PoolState;
import schultz.thomas.schub.core.team.data.model.Team;
import schultz.thomas.schub.core.team.data.model.TeamMember;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * <strong>Le panneau 3 — le pool de champions d'une équipe</strong> (plan §D.5).
 *
 * <p>Cinq colonnes, une par poste, croisant les maîtrises de chaque membre avec le catalogue des
 * champions. C'est le premier panneau utile du chantier D : il ne dépend d'aucune ingestion de
 * parties, et le pool vient des maîtrises, jamais de l'historique.</p>
 *
 * <h2>Ce que ce service décide, et que le connecteur ne peut pas décider</h2>
 *
 * <p>Le connecteur rend des maîtrises ; ranger un joueur sous un poste est un jugement de
 * domaine, et il se fonde sur le rôle porté par {@link TeamMember} — pas sur une déduction à
 * partir des champions. Un joueur qui maîtrise Lux n'est pas pour autant milieu, et deviner le
 * poste à la place du capitaine donnerait un effectif que personne ne reconnaît.</p>
 *
 * <h2>Trois refus de faire simple</h2>
 *
 * <ol>
 *   <li><strong>Aucun cache ici.</strong> Le connecteur détient déjà les quatre politiques du
 *       plan §D.2 ter. Un second cache dans le cœur, c'est deux vérités et une divergence
 *       garantie.</li>
 *   <li><strong>Aucun membre n'est perdu.</strong> Sans {@code puuid}, sans maîtrises, sans
 *       poste : il est rendu avec l'état qui dit pourquoi. Faire échouer la requête pour un seul
 *       joueur non lié rendrait le panneau inutilisable pour les quatre autres.</li>
 *   <li><strong>Pas de catalogue, pas un seul champion.</strong> Les noms et les icônes n'ont de
 *       sens que rapportés à une version de Data Dragon ; en rendre sans elle produirait une
 *       donnée fausse dans trois mois, invisible aujourd'hui.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChampionPoolService {

    /**
     * Combien de champions par membre si l'appelant ne demande rien.
     *
     * <p>Une colonne n'affiche pas cent soixante champions, et les maîtrises sont rendues du plus
     * au moins maîtrisé : au-delà d'une dizaine, on liste des champions joués une fois.</p>
     */
    public static final int CHAMPIONS_PAR_MEMBRE_DEFAUT = 10;

    /** Au-delà, ce n'est plus un pool mais un export — et cinq requêtes d'autant plus lourdes. */
    public static final int CHAMPIONS_PAR_MEMBRE_MAX = 50;

    private final TeamService teamService;
    private final MemberDirectory memberDirectory;
    private final RiotChampionGateway championGateway;

    /**
     * Le pool d'une équipe, pour un lecteur qui a le droit de la voir.
     *
     * <p>L'autorisation n'est pas réécrite ici : {@code requireVisible} pose la question
     * {@code TEAM_VIEW} <em>sur cette équipe</em> au seul évaluateur du système, qui interroge
     * {@link TeamScopedAuthority} pour savoir ce que l'appartenance donne. Un non-membre est
     * refusé par la même chaîne que partout ailleurs (plan §A.1).</p>
     */
    public ChampionPoolDto of(User actor, String teamId, Integer championsDemandes) {
        Team team = teamService.requireVisible(actor, teamId);
        int parMembre = borne(championsDemandes);

        List<TeamMember> joueurs = joueursDe(team);
        Optional<RiotChampionGateway.Catalogue> catalogue =
                joueurs.isEmpty() ? Optional.empty() : championGateway.catalogue();
        Map<String, ChampionPoolMemberDto> parMembreId =
                projette(joueurs, catalogue, parMembre);

        List<ChampionPoolColumnDto> colonnes = new ArrayList<>();
        for (GameRole role : GameRole.values()) {
            colonnes.add(new ChampionPoolColumnDto(role, joueurs.stream()
                    .filter(membre -> membre.getRole() == role)
                    .map(membre -> parMembreId.get(membre.getMemberId()))
                    .toList()));
        }
        List<ChampionPoolMemberDto> sansPoste = joueurs.stream()
                .filter(membre -> membre.getRole() == null)
                .map(membre -> parMembreId.get(membre.getMemberId()))
                .toList();

        return new ChampionPoolDto(
                team.getId(),
                team.getName(),
                catalogue.map(RiotChampionGateway.Catalogue::version).orElse(null),
                parMembre,
                colonnes,
                sansPoste,
                placeDuLecteur(team, actor),
                Instant.now());
    }

    // --- interne ---

    /**
     * Qui entre dans le panneau.
     *
     * <p><strong>Les coachs en sortent</strong>, et c'est le seul retrait : ils n'ont pas de
     * poste, ne sont jamais retenus dans une composition, et leur maîtrise de champions ne dit
     * rien de ce que l'équipe peut aligner. Les remplaçants restent — c'est justement ce qu'on
     * regarde pour préparer une rotation.</p>
     */
    private static List<TeamMember> joueursDe(Team team) {
        if (team.getMembers() == null) {
            return List.of();
        }
        return team.getMembers().stream()
                .filter(membre -> membre.getStatus() != MemberStatus.COACH)
                .sorted(Comparator
                        .comparing((TeamMember membre) -> membre.getStatus() != MemberStatus.TITULAIRE)
                        .thenComparing(membre -> Optional.ofNullable(membre.riotId()).orElse(""),
                                String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /**
     * Un appel de maîtrises par {@code puuid} distinct, et un seul aller-retour vers l'identité
     * pour toute l'équipe — jamais un par membre, ce qui est exactement ce que le front ne doit
     * pas avoir à faire non plus.
     */
    private Map<String, ChampionPoolMemberDto> projette(
            List<TeamMember> joueurs,
            Optional<RiotChampionGateway.Catalogue> catalogue, int parMembre) {

        Map<String, MemberDirectory.MemberIdentity> identites = resoutLesComptes(joueurs);
        Map<String, Optional<List<RiotChampionGateway.Mastery>>> maitrisesParPuuid = new HashMap<>();

        Map<String, ChampionPoolMemberDto> projections = new HashMap<>();
        for (TeamMember membre : joueurs) {
            projections.put(membre.getMemberId(),
                    projette(membre, identites, catalogue, maitrisesParPuuid, parMembre));
        }
        return projections;
    }

    private ChampionPoolMemberDto projette(
            TeamMember membre,
            Map<String, MemberDirectory.MemberIdentity> identites,
            Optional<RiotChampionGateway.Catalogue> catalogue,
            Map<String, Optional<List<RiotChampionGateway.Mastery>>> maitrisesParPuuid,
            int parMembre) {

        String puuid = membre.getRiotPuuid();
        PoolState state;
        List<ChampionPoolEntryDto> champions = List.of();
        Instant observedAt = null;

        if (puuid == null || puuid.isBlank()) {
            state = PoolState.COMPTE_RIOT_ABSENT;
        } else if (catalogue.isEmpty()) {
            // Sans catalogue, on n'a ni nom, ni icône, ni version : demander les maîtrises
            // coûterait un appel pour une donnée qu'on ne pourrait pas servir.
            state = PoolState.CATALOGUE_INDISPONIBLE;
        } else {
            Optional<List<RiotChampionGateway.Mastery>> maitrises = maitrisesParPuuid
                    .computeIfAbsent(puuid, p -> championGateway.masteries(p, parMembre));
            if (maitrises.isEmpty()) {
                state = PoolState.MAITRISES_INDISPONIBLES;
            } else if (maitrises.get().isEmpty()) {
                state = PoolState.AUCUNE_MAITRISE;
            } else {
                state = PoolState.MAITRISES_CONNUES;
                champions = croise(maitrises.get(), catalogue.get(), parMembre);
                observedAt = maitrises.get().stream()
                        .map(RiotChampionGateway.Mastery::observedAt)
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(null);
            }
        }

        MemberDirectory.MemberIdentity identite =
                membre.getUserId() == null ? null : identites.get(membre.getUserId());
        return new ChampionPoolMemberDto(
                membre.getMemberId(),
                nomAffiche(membre, identite),
                identite == null ? null : identite.avatarUrl(),
                membre.getRiotGameName(),
                membre.getRiotTagLine(),
                membre.getStatus(),
                membre.isLinked(),
                state,
                champions,
                observedAt);
    }

    /**
     * Croise les maîtrises au catalogue, <strong>sans jamais écarter une maîtrise</strong>.
     *
     * <p>Un champion absent du catalogue est un champion sorti après la version servie. Le
     * supprimer ferait disparaître une maîtrise réelle et donnerait une colonne plus courte sans
     * explication ; il est donc rendu avec son seul identifiant numérique, et c'est l'écran qui
     * décide comment dessiner une case sans icône.</p>
     *
     * <p>L'ordre du connecteur est conservé — du plus maîtrisé au moins maîtrisé. Le
     * {@code limit} est déjà passé à l'appel ; la borne est réappliquée ici parce qu'un
     * connecteur qui rendrait plus que demandé ne doit pas faire gonfler la réponse.</p>
     */
    private static List<ChampionPoolEntryDto> croise(
            List<RiotChampionGateway.Mastery> maitrises,
            RiotChampionGateway.Catalogue catalogue,
            int parMembre) {

        return maitrises.stream()
                .limit(parMembre)
                .map(maitrise -> {
                    RiotChampionGateway.Champion champion = catalogue.parId().get(maitrise.championId());
                    return new ChampionPoolEntryDto(
                            maitrise.championId(),
                            champion == null ? null : champion.key(),
                            champion == null ? null : champion.name(),
                            champion == null ? null : champion.iconUrl(),
                            maitrise.level(),
                            maitrise.points(),
                            maitrise.lastPlayedAt());
                })
                .toList();
    }

    /** Le pseudo du compte quand le membre est lié, son Riot ID sinon — jamais recopié en base. */
    private static String nomAffiche(TeamMember membre, MemberDirectory.MemberIdentity identite) {
        if (identite != null && identite.displayName() != null && !identite.displayName().isBlank()) {
            return identite.displayName();
        }
        return membre.riotId();
    }

    private Map<String, MemberDirectory.MemberIdentity> resoutLesComptes(List<TeamMember> joueurs) {
        Set<String> ids = new HashSet<>();
        joueurs.stream()
                .map(TeamMember::getUserId)
                .filter(id -> id != null && !id.isBlank())
                .forEach(ids::add);
        return ids.isEmpty() ? Map.of() : memberDirectory.byIds(ids);
    }

    private static String placeDuLecteur(Team team, User actor) {
        if (actor == null || actor.getId() == null || team.getMembers() == null) {
            return null;
        }
        return team.getMembers().stream()
                .filter(membre -> actor.getId().equals(membre.getUserId()))
                .map(TeamMember::getMemberId)
                .findFirst()
                .orElse(null);
    }

    /**
     * Borne la demande plutôt que de la refuser : un {@code ?champions=500} est une maladresse
     * d'appelant, pas une raison de ne rien afficher. La valeur réellement appliquée est rendue
     * dans la réponse, donc le silence n'en est pas un.
     */
    private static int borne(Integer demande) {
        if (demande == null) {
            return CHAMPIONS_PAR_MEMBRE_DEFAUT;
        }
        return Math.clamp(demande.longValue(), 1, CHAMPIONS_PAR_MEMBRE_MAX);
    }
}
