package schultz.thomas.schub.core.team.business.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import schultz.thomas.schub.core.business.service.RiotConnectorService;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.team.api.dto.StatsRefreshDto;
import schultz.thomas.schub.core.team.data.model.Team;
import schultz.thomas.schub.core.team.data.model.TeamMember;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

// Les statistiques se recalculent à chaque lecture : mettre à jour, c'est seulement aller chercher les parties.
@Slf4j
@Service
@RequiredArgsConstructor
public class TeamStatsRefreshService {

    static final Duration DELAI = Duration.ofMinutes(3);

    private final TeamService teamService;
    private final RiotConnectorService riotConnector;
    private final MongoTemplate mongo;

    public StatsRefreshDto status(User actor, String teamId) {
        Team team = teamService.requireVisible(actor, teamId);
        return new StatsRefreshDto(false, 0, prochaine(team.getStatsRefreshedAt(), Instant.now()));
    }

    public StatsRefreshDto refresh(User actor, String teamId) {
        Team team = teamService.requireVisible(actor, teamId);
        Instant maintenant = Instant.now();

        // Réservation atomique : deux clics simultanés ne déclenchent qu'une collecte.
        Team reservee = mongo.findAndModify(
                Query.query(Criteria.where("_id").is(teamId).orOperator(
                        Criteria.where("statsRefreshedAt").is(null),
                        Criteria.where("statsRefreshedAt").lte(maintenant.minus(DELAI)))),
                new Update().set("statsRefreshedAt", maintenant),
                FindAndModifyOptions.options().returnNew(true),
                Team.class);
        if (reservee == null) {
            return new StatsRefreshDto(false, 0, prochaine(team.getStatsRefreshedAt(), maintenant));
        }

        int demandes = (int) TeamPlayerStatsService.joueursDe(team).stream()
                .map(TeamMember::getRiotPuuid)
                .filter(Objects::nonNull)
                .filter(puuid -> !puuid.isBlank())
                .distinct()
                .filter(riotConnector::requestIngest)
                .count();
        log.info("Mise à jour des stats de l'équipe « {} » : collecte demandée pour {} joueur(s)",
                team.getName(), demandes);
        return new StatsRefreshDto(true, demandes, maintenant.plus(DELAI));
    }

    private static Instant prochaine(Instant derniere, Instant maintenant) {
        if (derniere == null) {
            return null;
        }
        Instant suivante = derniere.plus(DELAI);
        return suivante.isAfter(maintenant) ? suivante : null;
    }
}
