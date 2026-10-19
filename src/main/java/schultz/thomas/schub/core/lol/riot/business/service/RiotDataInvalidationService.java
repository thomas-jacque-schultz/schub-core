package schultz.thomas.schub.core.lol.riot.business.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import schultz.thomas.schub.core.augur.data.repository.FindingRecordRepository;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.data.repository.UserRepository;
import schultz.thomas.schub.core.lol.riot.business.model.IngestPauseState;
import schultz.thomas.schub.core.lol.riot.business.model.RiotDataInvalidation;
import schultz.thomas.schub.core.lol.riot.business.model.RiotDataInventory;
import schultz.thomas.schub.core.lol.team.business.service.StalePuuidRepair;
import schultz.thomas.schub.core.lol.team.data.model.TeamMember;
import schultz.thomas.schub.core.lol.team.data.repository.TeamRepository;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

// La file d'ingest doit être en pause : le connecteur refuse sinon. Il jette tout ce qu'il a collecté, le cœur les
// constats calculés dessus. Restent les utilisateurs, les équipes, les compositions, les pools et les revues (attachées à
// l'identifiant de partie, stable d'une clé à l'autre). Les comptes liés et les places d'équipe sont ensuite résolus de
// nouveau par leur Riot ID ; leur collecte part à la reprise de l'ingest.
@Slf4j
@Service
@RequiredArgsConstructor
public class RiotDataInvalidationService {

    public static final String CONFIRMATION = "INVALIDER";

    private final RiotDataGateway riotData;
    private final StalePuuidRepair repair;
    private final FindingRecordRepository findings;
    private final UserRepository users;
    private final TeamRepository teams;

    public IngestPauseState pause() {
        return riotData.pause().map(IngestPauseState::from).orElseGet(IngestPauseState::unavailable);
    }

    public IngestPauseState setPause(boolean paused) {
        return IngestPauseState.from(riotData.setPause(paused));
    }

    public RiotDataInventory inventory() {
        Optional<RiotDataGateway.Inventory> connecteur = riotData.inventory();
        return new RiotDataInventory(connecteur.isPresent(),
                connecteur.map(inventaire -> somme(inventaire.purged())).orElse(0L), findings.count(),
                users.countByRiotPuuidNotNull(), placesSuivies(), teams.count());
    }

    public RiotDataInvalidation invalidate(User actor, String confirmation) {
        if (!CONFIRMATION.equals(confirmation)) {
            throw new IllegalArgumentException("Saisis " + CONFIRMATION + " pour confirmer l'invalidation.");
        }
        Set<String> suivis = repair.puuidsSuivis();
        long documents = somme(riotData.purge());
        long constats = findings.count();
        findings.deleteAll();
        log.warn("Données Riot invalidées par l'utilisateur {} : {} documents Riot et {} constats effacés, "
                + "{} puuid à résoudre de nouveau", actor.getId(), documents, constats, suivis.size());
        repair.resoutTout(suivis);
        return new RiotDataInvalidation(documents, constats, suivis.size());
    }

    private long placesSuivies() {
        return teams.findRiotPuuids().stream().flatMap(team -> team.getMembers().stream())
                .map(TeamMember::getRiotPuuid).filter(puuid -> puuid != null && !puuid.isBlank()).count();
    }

    private static long somme(Map<String, Long> parCollection) {
        return parCollection.values().stream().mapToLong(Long::longValue).sum();
    }
}
