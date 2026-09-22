package schultz.thomas.schub.core.team.business.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import schultz.thomas.schub.core.business.model.Permission;
import schultz.thomas.schub.core.business.service.PermissionEvaluator;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.team.api.dto.ChampionCatalogEntryDto;
import schultz.thomas.schub.core.team.api.dto.ChampionPoolColumnDto;
import schultz.thomas.schub.core.team.api.dto.ChampionPoolDto;
import schultz.thomas.schub.core.team.api.dto.ChampionPoolEntryDto;
import schultz.thomas.schub.core.team.api.dto.ChampionPoolMemberDto;
import schultz.thomas.schub.core.team.business.model.GameRole;
import schultz.thomas.schub.core.team.business.model.MemberStatus;
import schultz.thomas.schub.core.team.business.model.PoolState;
import schultz.thomas.schub.core.team.data.model.Team;
import schultz.thomas.schub.core.team.data.model.TeamChampionPool;
import schultz.thomas.schub.core.team.data.model.TeamMember;
import schultz.thomas.schub.core.team.data.repository.TeamChampionPoolRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * <strong>Le panneau 3 — ce que l'équipe peut aligner à chaque poste</strong> (plan §D.5).
 *
 * <h2>La question que ce service répond</h2>
 *
 * <p>Pas « qu'est-ce que chacun maîtrise ? » — ça, c'est la matière première, et le connecteur la
 * détient. La question est « <em>qu'est-ce qu'on peut poser à ce poste ?</em> », et elle demande
 * trois choses qu'aucune API ne donne : un <strong>choix</strong> de champions par poste, fait par
 * l'équipe et gardé ; la liste des membres <strong>qui tiennent ce poste</strong> ; un
 * <strong>plancher</strong> en dessous duquel on ne compte pas quelqu'un sur un champion.</p>
 *
 * <h2>Le plancher appartient à l'équipe</h2>
 *
 * <p>Il change la réponse, donc il ne peut pas être un réglage d'affichage : deux membres qui
 * regardent le même poste avec deux planchers différents liraient deux listes et se croiraient
 * d'accord. Il est enregistré, et un plancher passé en paramètre de lecture ne sert qu'à voir —
 * il n'écrit rien, et la réponse dit les deux valeurs.</p>
 *
 * <h2>Personne ne disparaît</h2>
 *
 * <p>Un membre sans compte Riot lié, un connecteur muet, un compte neuf sans maîtrise : le membre
 * est rendu dans sa colonne, avec l'état qui dit pourquoi on ne sait rien de lui. Le taire ferait
 * chercher pendant dix minutes pourquoi il manque quelqu'un.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChampionPoolService {

    private final TeamService teamService;
    private final MemberDirectory memberDirectory;
    private final RiotChampionGateway championGateway;
    private final TeamChampionPoolRepository pools;
    private final PermissionEvaluator permissionEvaluator;

    // --- lecture ---

    /**
     * @param plancherDemande un plancher à appliquer pour cette lecture seulement, ou {@code null}
     *                        pour celui de l'équipe
     */
    public ChampionPoolDto of(User actor, String teamId, Integer plancherDemande) {
        Team team = teamService.requireVisible(actor, teamId);
        TeamChampionPool pool = poolDe(teamId);
        int plancher = plancherDemande == null ? pool.getMasteryFloor() : Math.max(0, plancherDemande);

        Optional<RiotChampionGateway.Catalogue> catalogue = championGateway.catalogue();
        List<TeamMember> joueurs = joueursDe(team);
        Map<String, Maitrises> maitrises = maitrisesDe(joueurs, catalogue.isPresent());
        Map<String, MemberDirectory.MemberIdentity> identites = resoutLesComptes(joueurs);

        List<ChampionPoolColumnDto> colonnes = new ArrayList<>();
        for (GameRole role : GameRole.values()) {
            colonnes.add(colonne(role, pool, joueurs, maitrises, identites, catalogue, plancher));
        }

        return new ChampionPoolDto(
                team.getId(),
                team.getName(),
                catalogue.map(RiotChampionGateway.Catalogue::version).orElse(null),
                plancher,
                pool.getMasteryFloor(),
                catalogue.map(ChampionPoolService::catalogue).orElseGet(List::of),
                colonnes,
                placeDuLecteur(team, actor),
                peutEcrire(actor, teamId),
                Instant.now());
    }

    // --- écriture ---

    /**
     * Remplace les champions retenus à un poste.
     *
     * <p>Les clés sont validées contre le catalogue : une clé inventée resterait en base sans
     * jamais rien afficher, et personne ne saurait d'où elle vient. Sans catalogue, l'écriture est
     * refusée plutôt que faite à l'aveugle.</p>
     */
    public ChampionPoolDto setChampions(User actor, String teamId, GameRole role, List<String> championKeys) {
        teamService.requireVisible(actor, teamId);
        permissionEvaluator.require(actor, Permission.COMPOSITION_EDIT, TeamService.ref(teamId));

        RiotChampionGateway.Catalogue catalogue = championGateway.catalogue().orElseThrow(
                () -> new IllegalStateException(
                        "Catalogue des champions indisponible : le choix ne peut pas être vérifié"));

        Set<String> connues = new HashSet<>();
        catalogue.parId().values().forEach(champion -> connues.add(champion.key()));

        Set<String> retenues = new LinkedHashSet<>();
        for (String key : championKeys == null ? List.<String>of() : championKeys) {
            String propre = key == null ? null : key.trim();
            if (propre == null || propre.isEmpty()) {
                continue;
            }
            if (!connues.contains(propre)) {
                throw new IllegalArgumentException("Aucun champion nommé « " + propre + " » dans ce patch");
            }
            retenues.add(propre);
        }
        TeamChampionPool pool = poolDe(teamId);
        pool.setChampionKeys(role, List.copyOf(retenues));
        pool.setUpdatedAt(Instant.now());
        pools.save(pool);
        log.info("Pool du poste {} de l'équipe {} : {} champion(s) retenu(s)", role, teamId, retenues.size());
        return of(actor, teamId, null);
    }

    public ChampionPoolDto setMasteryFloor(User actor, String teamId, int masteryFloor) {
        teamService.requireVisible(actor, teamId);
        permissionEvaluator.require(actor, Permission.COMPOSITION_EDIT, TeamService.ref(teamId));
        if (masteryFloor < 0) {
            throw new IllegalArgumentException("Un plancher de maîtrise ne peut pas être négatif");
        }

        TeamChampionPool pool = poolDe(teamId);
        pool.setMasteryFloor(masteryFloor);
        pool.setUpdatedAt(Instant.now());
        pools.save(pool);
        return of(actor, teamId, null);
    }

    // --- interne ---

    private TeamChampionPool poolDe(String teamId) {
        return pools.findById(teamId).orElseGet(() -> {
            TeamChampionPool neuf = new TeamChampionPool();
            neuf.setTeamId(teamId);
            return neuf;
        });
    }

    private boolean peutEcrire(User actor, String teamId) {
        return permissionEvaluator.can(actor, Permission.COMPOSITION_EDIT, TeamService.ref(teamId));
    }

    private ChampionPoolColumnDto colonne(
            GameRole role,
            TeamChampionPool pool,
            List<TeamMember> joueurs,
            Map<String, Maitrises> maitrises,
            Map<String, MemberDirectory.MemberIdentity> identites,
            Optional<RiotChampionGateway.Catalogue> catalogue,
            int plancher) {

        List<TeamMember> duPoste = joueurs.stream().filter(membre -> membre.playsRole(role)).toList();

        List<ChampionPoolMemberDto> muets = duPoste.stream()
                .filter(membre -> etat(membre, maitrises) != PoolState.MAITRISES_CONNUES)
                .map(membre -> projette(membre, maitrises, identites, null))
                .toList();

        if (catalogue.isEmpty()) {
            return new ChampionPoolColumnDto(role, List.of(), muets, 0);
        }

        Map<String, RiotChampionGateway.Champion> parCle = new HashMap<>();
        catalogue.get().parId().values().forEach(champion -> parCle.put(champion.key(), champion));

        List<ChampionPoolEntryDto> champions = new ArrayList<>();
        int masques = 0;
        for (String key : pool.championKeys(role)) {
            RiotChampionGateway.Champion champion = parCle.get(key);
            if (champion == null) {
                // Retenu sous un patch qui le connaissait, absent de celui-ci. Le taire ferait
                // disparaître un choix que personne n'a défait.
                champions.add(new ChampionPoolEntryDto(0, key, null, null, List.of(), 0));
                continue;
            }
            ChampionPoolEntryDto entree = entree(champion, duPoste, maitrises, identites, plancher);
            // Un champion que personne du poste ne tient au-dessus du plancher n'est pas alignable :
            // il encombre la colonne sans rien dire. Le choix reste enregistré, seul l'affichage
            // l'écarte — le compte dit qu'ils existent, sinon baisser le plancher serait un pari.
            if (entree.players().isEmpty() && entree.name() != null) {
                masques++;
                continue;
            }
            champions.add(entree);
        }

        champions.sort(Comparator.comparingDouble(ChampionPoolService::maitriseMoyenne).reversed()
                .thenComparing(entree -> entree.name() == null ? "" : entree.name()));

        return new ChampionPoolColumnDto(role, champions, muets, masques);
    }

    /** Sans joueur retenu — champion hors catalogue — la moyenne vaut zéro et l'entrée finit la liste. */
    private static double maitriseMoyenne(ChampionPoolEntryDto entree) {
        return entree.players().stream()
                .map(ChampionPoolMemberDto::masteryPoints)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0d);
    }

    private ChampionPoolEntryDto entree(
            RiotChampionGateway.Champion champion,
            List<TeamMember> duPoste,
            Map<String, Maitrises> maitrises,
            Map<String, MemberDirectory.MemberIdentity> identites,
            int plancher) {

        List<ChampionPoolMemberDto> retenus = new ArrayList<>();
        int ecartes = 0;

        for (TeamMember membre : duPoste) {
            Maitrises siennes = maitrises.get(membre.getMemberId());
            if (siennes == null || siennes.state() != PoolState.MAITRISES_CONNUES) {
                continue;
            }
            RiotChampionGateway.Mastery maitrise = siennes.parChampion().get(champion.id());
            int points = maitrise == null ? 0 : maitrise.points();
            if (points == 0) {
                // Jamais touché ce champion : ce n'est pas un candidat que le plancher écarte,
                // c'est un non-candidat. Le compter comme écarté ferait croire à un réglage trop
                // haut là où il n'y a rien à régler.
                continue;
            }
            if (points < plancher) {
                ecartes++;
                continue;
            }
            retenus.add(projette(membre, maitrises, identites, maitrise));
        }

        retenus.sort(Comparator.comparingInt(
                (ChampionPoolMemberDto joueur) -> joueur.masteryPoints() == null ? 0 : joueur.masteryPoints())
                .reversed());

        return new ChampionPoolEntryDto(champion.id(), champion.key(), champion.name(),
                champion.iconUrl(), retenus, ecartes);
    }

    private ChampionPoolMemberDto projette(
            TeamMember membre,
            Map<String, Maitrises> maitrises,
            Map<String, MemberDirectory.MemberIdentity> identites,
            RiotChampionGateway.Mastery maitrise) {

        Maitrises siennes = maitrises.get(membre.getMemberId());
        MemberDirectory.MemberIdentity identite =
                membre.getUserId() == null ? null : identites.get(membre.getUserId());

        Integer points = siennes != null && siennes.state() == PoolState.MAITRISES_CONNUES
                ? (maitrise == null ? 0 : maitrise.points())
                : null;

        return new ChampionPoolMemberDto(
                membre.getMemberId(),
                nomAffiche(membre, identite),
                identite == null ? null : identite.avatarUrl(),
                membre.getRiotGameName(),
                membre.getRiotTagLine(),
                membre.getStatus(),
                membre.isLinked(),
                etat(membre, maitrises),
                maitrise == null ? null : maitrise.level(),
                points,
                maitrise == null ? null : maitrise.lastPlayedAt(),
                siennes == null ? null : siennes.observedAt());
    }

    private static PoolState etat(TeamMember membre, Map<String, Maitrises> maitrises) {
        Maitrises siennes = maitrises.get(membre.getMemberId());
        return siennes == null ? PoolState.COMPTE_RIOT_ABSENT : siennes.state();
    }

    /**
     * Les coachs sortent du panneau — ils ne tiennent aucun poste et ne sont jamais alignés. Les
     * remplaçants restent : c'est justement ce qu'on regarde pour préparer une rotation.
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
     * Un appel de maîtrises par {@code puuid} distinct, et <strong>toutes</strong> les maîtrises :
     * un « top 10 » répondrait « aucune maîtrise » sur le onzième champion retenu à un poste.
     */
    private Map<String, Maitrises> maitrisesDe(List<TeamMember> joueurs, boolean catalogueLa) {
        Map<String, Maitrises> parPuuid = new HashMap<>();
        Map<String, Maitrises> parMembre = new HashMap<>();

        for (TeamMember membre : joueurs) {
            String puuid = membre.getRiotPuuid();
            if (puuid == null || puuid.isBlank()) {
                parMembre.put(membre.getMemberId(), Maitrises.sans(PoolState.COMPTE_RIOT_ABSENT));
                continue;
            }
            if (!catalogueLa) {
                parMembre.put(membre.getMemberId(), Maitrises.sans(PoolState.CATALOGUE_INDISPONIBLE));
                continue;
            }
            parMembre.put(membre.getMemberId(),
                    parPuuid.computeIfAbsent(puuid, p -> lit(championGateway.masteries(p))));
        }
        return parMembre;
    }

    private static Maitrises lit(Optional<List<RiotChampionGateway.Mastery>> reponse) {
        if (reponse.isEmpty()) {
            return Maitrises.sans(PoolState.MAITRISES_INDISPONIBLES);
        }
        if (reponse.get().isEmpty()) {
            return Maitrises.sans(PoolState.AUCUNE_MAITRISE);
        }
        Map<Integer, RiotChampionGateway.Mastery> parChampion = new HashMap<>();
        Instant observedAt = null;
        for (RiotChampionGateway.Mastery maitrise : reponse.get()) {
            parChampion.put(maitrise.championId(), maitrise);
            if (observedAt == null) {
                observedAt = maitrise.observedAt();
            }
        }
        return new Maitrises(PoolState.MAITRISES_CONNUES, parChampion, observedAt);
    }

    private static List<ChampionCatalogEntryDto> catalogue(RiotChampionGateway.Catalogue catalogue) {
        return catalogue.parId().values().stream()
                .map(champion -> new ChampionCatalogEntryDto(
                        champion.id(), champion.key(), champion.name(), champion.iconUrl()))
                .sorted(Comparator.comparing(
                        entree -> Optional.ofNullable(entree.name()).orElse(entree.championKey()),
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

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

    private record Maitrises(PoolState state,
                             Map<Integer, RiotChampionGateway.Mastery> parChampion,
                             Instant observedAt) {

        static Maitrises sans(PoolState state) {
            return new Maitrises(state, Map.of(), null);
        }
    }
}
