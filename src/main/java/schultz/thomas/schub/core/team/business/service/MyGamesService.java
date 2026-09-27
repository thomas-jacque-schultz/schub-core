package schultz.thomas.schub.core.team.business.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import schultz.thomas.schub.core.business.service.RiotConnectorUnavailableException;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.team.api.dto.MyGamesDto;
import schultz.thomas.schub.core.team.api.dto.TeamGameDetailDto;
import schultz.thomas.schub.core.team.business.model.PlayerRef;
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
        return games(joueur(actor), days, limit);
    }

    public TeamGameDetailDto game(User actor, String matchId, Integer days) {
        return game(joueur(actor), matchId, days);
    }

    public MyGamesDto games(PlayerRef joueur, Integer days, Integer limit) {
        String puuid = puuid(joueur);
        if (puuid == null) {
            return vide(joueur, days, StatsState.COMPTE_RIOT_ABSENT);
        }
        int borne = limit == null ? PARTIES_DEFAUT : (int) Math.clamp(limit.longValue(), 1, PARTIES_MAX);
        Optional<RiotStatsGateway.SharedMatches> parties =
                statsGateway.playerMatches(puuid, PlayerStatsService.depuis(days), borne);
        if (parties.isEmpty()) {
            return vide(joueur, days, StatsState.CONNECTEUR_INDISPONIBLE);
        }
        if (parties.get().matches().isEmpty()) {
            return vide(joueur, days, etatSansPartie(puuid));
        }
        return new MyGamesDto(days, StatsState.STATISTIQUES_CONNUES,
                views.parties(parties.get().matches(), soi(joueur), noms(joueur)),
                parties.get().totalMatches(), parties.get().truncated(), joueur.id(), Instant.now());
    }

    public TeamGameDetailDto game(PlayerRef joueur, String matchId, Integer days) {
        String puuid = puuid(joueur);
        if (puuid == null || matchId == null || matchId.isBlank()) {
            throw new NoSuchElementException("Aucune partie « " + matchId + " » pour ce compte");
        }
        RiotStatsGateway.SharedMatch partie = statsGateway.sharedMatchesAmong(List.of(puuid), 1, List.of(matchId))
                .orElseThrow(RiotConnectorUnavailableException::new)
                .matches().stream()
                .filter(candidate -> matchId.equals(candidate.matchId()))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Aucune partie « " + matchId + " » pour ce compte"));
        return views.detail(null, partie, soi(joueur), noms(joueur), joueur.id(), days);
    }

    private StatsState etatSansPartie(String puuid) {
        return statsGateway.coverage(List.of(puuid)).orElseGet(List::of).stream()
                .filter(ligne -> ligne.tracked() || ligne.knownMatches() > 0)
                .findFirst()
                .map(ligne -> StatsState.INGESTION_EN_COURS)
                .orElse(StatsState.AUCUNE_PARTIE);
    }

    private PlayerRef joueur(User actor) {
        MemberDirectory.MemberIdentity identite = memberDirectory.byIds(Set.of(actor.getId())).get(actor.getId());
        String nom = identite != null && identite.displayName() != null && !identite.displayName().isBlank()
                ? identite.displayName() : actor.getRiotGameName();
        return new PlayerRef(actor.getId(), nom, actor.getRiotPuuid(), actor.getRiotGameName(), actor.getRiotTagLine());
    }

    private static MyGamesDto vide(PlayerRef joueur, Integer days, StatsState state) {
        return new MyGamesDto(days, state, List.of(), 0, false, joueur.id(), Instant.now());
    }

    private static String puuid(PlayerRef joueur) {
        String puuid = joueur.puuid();
        return puuid == null || puuid.isBlank() ? null : puuid;
    }

    private static Map<String, TeamMember> soi(PlayerRef joueur) {
        TeamMember moi = new TeamMember();
        moi.setMemberId(joueur.id());
        moi.setUserId(joueur.id());
        moi.setRiotPuuid(joueur.puuid());
        moi.setRiotGameName(joueur.gameName());
        moi.setRiotTagLine(joueur.tagLine());
        return Map.of(joueur.puuid(), moi);
    }

    private static Map<String, String> noms(PlayerRef joueur) {
        return joueur.displayName() == null ? Map.of() : Map.of(joueur.id(), joueur.displayName());
    }
}
