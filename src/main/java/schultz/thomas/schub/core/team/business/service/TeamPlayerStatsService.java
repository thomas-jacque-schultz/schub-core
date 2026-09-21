package schultz.thomas.schub.core.team.business.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.team.api.dto.PlayerStatsDto;
import schultz.thomas.schub.core.team.api.dto.StatLineDto;
import schultz.thomas.schub.core.team.api.dto.TeamComparisonDto;
import schultz.thomas.schub.core.team.api.dto.TeamPlayersStatsDto;
import schultz.thomas.schub.core.team.business.model.GameRole;
import schultz.thomas.schub.core.team.business.model.MemberStatus;
import schultz.thomas.schub.core.team.business.model.StatsState;
import schultz.thomas.schub.core.team.data.model.Team;
import schultz.thomas.schub.core.team.data.model.TeamMember;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * <strong>Le panneau 1 — les joueurs d'une équipe</strong> (plan §D.8).
 *
 * <p>Une colonne par joueur. La lecture est gardée par {@code TEAM_VIEW} <em>sur cette
 * équipe</em> : la permission est à portée de ressource, elle vient de l'appartenance et pas du
 * rôle, et c'est {@link TeamService#requireVisible} qui la réclame.</p>
 *
 * <p>Aucun {@code puuid} ne sort d'ici. Un identifiant de membre suffit à l'écran, et un puuid
 * rendu au client serait la clé d'entrée d'un sondage d'historique.</p>
 */
@Service
@RequiredArgsConstructor
public class TeamPlayerStatsService {

    private final TeamService teamService;
    private final MemberDirectory memberDirectory;
    private final PlayerStatsService playerStatsService;

    public TeamPlayersStatsDto of(User actor, String teamId, Integer days, Integer champions) {
        Team team = teamService.requireVisible(actor, teamId);
        List<TeamMember> joueurs = joueursDe(team);
        int championsMax = PlayerStatsService.bornerChampions(champions);
        Instant since = PlayerStatsService.depuis(days);

        List<String> puuids = joueurs.stream()
                .map(TeamMember::getRiotPuuid)
                .filter(puuid -> puuid != null && !puuid.isBlank())
                .distinct()
                .toList();
        Map<String, PlayerStatsService.Figures> figures =
                playerStatsService.of(puuids, since, championsMax);
        Map<String, MemberDirectory.MemberIdentity> identites = resoutLesComptes(joueurs);

        List<PlayerStatsDto> colonnes = new ArrayList<>();
        for (TeamMember membre : joueurs) {
            colonnes.add(colonne(membre, figures, identites));
        }
        List<PlayerStatsDto> compares = comparent(colonnes);

        return new TeamPlayersStatsDto(team.getId(), team.getName(), days, championsMax, compares,
                placeDuLecteur(team, actor), Instant.now());
    }

    // --- interne ---

    static List<TeamMember> joueursDe(Team team) {
        if (team.getMembers() == null) {
            return List.of();
        }
        return team.getMembers().stream()
                .filter(membre -> membre.getStatus() != MemberStatus.COACH)
                .sorted(Comparator
                        .comparingInt((TeamMember membre) -> rang(membre.mainRole()))
                        .thenComparing(membre -> membre.getStatus() != MemberStatus.TITULAIRE)
                        .thenComparing(membre -> Optional.ofNullable(membre.riotId()).orElse(""),
                                String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private static int rang(GameRole role) {
        return role == null ? GameRole.values().length : role.ordinal();
    }

    private PlayerStatsDto colonne(TeamMember membre,
                                   Map<String, PlayerStatsService.Figures> figures,
                                   Map<String, MemberDirectory.MemberIdentity> identites) {
        String puuid = membre.getRiotPuuid();
        PlayerStatsService.Figures chiffres =
                puuid == null || puuid.isBlank() ? null : figures.get(puuid);
        MemberDirectory.MemberIdentity identite =
                membre.getUserId() == null ? null : identites.get(membre.getUserId());
        StatsState state = chiffres == null ? StatsState.COMPTE_RIOT_ABSENT : chiffres.state();

        return new PlayerStatsDto(
                membre.getMemberId(),
                nomAffiche(membre, identite),
                identite == null ? null : identite.avatarUrl(),
                membre.getRiotGameName(),
                membre.getRiotTagLine(),
                membre.getStatus(),
                membre.getRoles(),
                membre.isLinked(),
                state,
                chiffres == null ? null : chiffres.coverage(),
                chiffres == null ? null : chiffres.overall(),
                chiffres == null ? List.of() : chiffres.champions(),
                chiffres == null ? List.of() : chiffres.positions(),
                chiffres == null ? List.of() : chiffres.queues(),
                chiffres == null ? List.of() : chiffres.months(),
                state == StatsState.STATISTIQUES_CONNUES ? playerStatsService.rankings(puuid)
                        : List.of(),
                null);
    }

    /**
     * L'écart de chacun à la moyenne des autres. Il n'entre que des colonnes qui ont des parties :
     * compter un joueur sans données comme un zéro tirerait la moyenne vers le bas et ferait
     * briller tous les autres.
     */
    private static List<PlayerStatsDto> comparent(List<PlayerStatsDto> colonnes) {
        List<PlayerStatsDto> avecChiffres = colonnes.stream()
                .filter(colonne -> colonne.overall() != null && colonne.overall().games() > 0)
                .toList();
        if (avecChiffres.size() < 2) {
            return colonnes;
        }
        return colonnes.stream()
                .map(colonne -> {
                    if (colonne.overall() == null || colonne.overall().games() == 0) {
                        return colonne;
                    }
                    List<StatLineDto> autres = avecChiffres.stream()
                            .filter(candidat -> !candidat.memberId().equals(colonne.memberId()))
                            .map(PlayerStatsDto::overall)
                            .toList();
                    return versus(colonne, autres);
                })
                .toList();
    }

    private static PlayerStatsDto versus(PlayerStatsDto colonne, List<StatLineDto> autres) {
        if (autres.isEmpty()) {
            return colonne;
        }
        StatLineDto mien = colonne.overall();
        TeamComparisonDto comparaison = new TeamComparisonDto(
                autres.size(),
                StatLines.ecart(mien.winRate(), moyenne(autres, StatLineDto::winRate)),
                StatLines.ecart(mien.kda(), moyenne(autres, StatLineDto::kda)),
                StatLines.ecart(mien.goldPerMinute(), moyenne(autres, StatLineDto::goldPerMinute)),
                StatLines.ecart(mien.damagePerMinute(), moyenne(autres, StatLineDto::damagePerMinute)),
                StatLines.ecart(mien.visionPerMinute(), moyenne(autres, StatLineDto::visionPerMinute)));
        return new PlayerStatsDto(colonne.memberId(), colonne.displayName(), colonne.avatarUrl(),
                colonne.riotGameName(), colonne.riotTagLine(), colonne.status(), colonne.roles(),
                colonne.linked(), colonne.state(), colonne.coverage(), colonne.overall(),
                colonne.champions(), colonne.positions(), colonne.queues(), colonne.months(),
                colonne.rankings(), comparaison);
    }

    private static Double moyenne(List<StatLineDto> lignes, Function<StatLineDto, Double> mesure) {
        return StatLines.moyenne(lignes.stream().map(mesure).toList());
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

    static String placeDuLecteur(Team team, User actor) {
        if (actor == null || actor.getId() == null || team.getMembers() == null) {
            return null;
        }
        return team.getMembers().stream()
                .filter(membre -> actor.getId().equals(membre.getUserId()))
                .map(TeamMember::getMemberId)
                .findFirst()
                .orElse(null);
    }

    static Map<String, TeamMember> parPuuid(List<TeamMember> joueurs) {
        Map<String, TeamMember> index = new LinkedHashMap<>();
        joueurs.forEach(membre -> {
            String puuid = membre.getRiotPuuid();
            if (puuid != null && !puuid.isBlank()) {
                index.putIfAbsent(puuid, membre);
            }
        });
        return index;
    }
}
