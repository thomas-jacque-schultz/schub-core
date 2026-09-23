package schultz.thomas.schub.core.team.business.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import schultz.thomas.schub.core.business.service.RiotConnectorUnavailableException;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.team.api.dto.MyGamesDto;
import schultz.thomas.schub.core.team.api.dto.TeamGameDetailDto;
import schultz.thomas.schub.core.team.business.model.StatsState;
import schultz.thomas.schub.core.team.data.model.TeamMember;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

// Toutes les parties du joueur connecté, vues comme une partie d'équipe dont il serait le seul membre.
@Service
@RequiredArgsConstructor
public class MyGamesService {

    public static final int PARTIES_DEFAUT = 100;
    public static final int PARTIES_MAX = 500;

    private final RiotStatsGateway statsGateway;
    private final MemberDirectory memberDirectory;
    private final GameViews views;

    public MyGamesDto games(User actor, Integer days, Integer limit) {
        String puuid = puuid(actor);
        if (puuid == null) {
            return vide(actor, days, StatsState.COMPTE_RIOT_ABSENT);
        }
        int borne = limit == null ? PARTIES_DEFAUT : (int) Math.clamp(limit.longValue(), 1, PARTIES_MAX);
        Optional<RiotStatsGateway.SharedMatches> parties =
                statsGateway.playerMatches(puuid, PlayerStatsService.depuis(days), borne);
        if (parties.isEmpty()) {
            return vide(actor, days, StatsState.CONNECTEUR_INDISPONIBLE);
        }
        if (parties.get().matches().isEmpty()) {
            return vide(actor, days, etatSansPartie(puuid));
        }
        return new MyGamesDto(days, StatsState.STATISTIQUES_CONNUES,
                views.parties(parties.get().matches(), soi(actor, puuid), noms(actor)),
                parties.get().totalMatches(), parties.get().truncated(), actor.getId(), Instant.now());
    }

    public TeamGameDetailDto game(User actor, String matchId, Integer days) {
        String puuid = puuid(actor);
        if (puuid == null || matchId == null || matchId.isBlank()) {
            throw new NoSuchElementException("Aucune partie « " + matchId + " » pour ce compte");
        }
        RiotStatsGateway.SharedMatch partie = statsGateway.sharedMatchesAmong(List.of(puuid), 1, List.of(matchId))
                .orElseThrow(RiotConnectorUnavailableException::new)
                .matches().stream()
                .filter(candidate -> matchId.equals(candidate.matchId()))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Aucune partie « " + matchId + " » pour ce compte"));
        return views.detail(null, partie, soi(actor, puuid), noms(actor), actor.getId(), days);
    }

    private StatsState etatSansPartie(String puuid) {
        return statsGateway.coverage(List.of(puuid)).orElseGet(List::of).stream()
                .filter(ligne -> ligne.tracked() || ligne.knownMatches() > 0)
                .findFirst()
                .map(ligne -> StatsState.INGESTION_EN_COURS)
                .orElse(StatsState.AUCUNE_PARTIE);
    }

    private static MyGamesDto vide(User actor, Integer days, StatsState state) {
        return new MyGamesDto(days, state, List.of(), 0, false, actor.getId(), Instant.now());
    }

    private static String puuid(User actor) {
        String puuid = actor.getRiotPuuid();
        return puuid == null || puuid.isBlank() ? null : puuid;
    }

    private static Map<String, TeamMember> soi(User actor, String puuid) {
        TeamMember moi = new TeamMember();
        moi.setMemberId(actor.getId());
        moi.setUserId(actor.getId());
        moi.setRiotPuuid(puuid);
        moi.setRiotGameName(actor.getRiotGameName());
        moi.setRiotTagLine(actor.getRiotTagLine());
        return Map.of(puuid, moi);
    }

    private Map<String, String> noms(User actor) {
        MemberDirectory.MemberIdentity identite = memberDirectory.byIds(Set.of(actor.getId())).get(actor.getId());
        String nom = identite != null && identite.displayName() != null && !identite.displayName().isBlank()
                ? identite.displayName() : actor.getRiotGameName();
        return nom == null ? Map.of() : Map.of(actor.getId(), nom);
    }
}
