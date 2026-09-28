package schultz.thomas.schub.core.team.business.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import schultz.thomas.schub.core.augur.data.repository.FindingRecordRepository;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.data.repository.UserRepository;
import schultz.thomas.schub.core.team.api.dto.IngestPauseDto;
import schultz.thomas.schub.core.team.api.dto.RiotDataInvalidationDto;
import schultz.thomas.schub.core.team.api.dto.RiotDataInventoryDto;
import schultz.thomas.schub.core.team.data.model.TeamMember;
import schultz.thomas.schub.core.team.data.repository.GameReviewRepository;
import schultz.thomas.schub.core.team.data.repository.TeamRepository;

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
    private final GameReviewRepository reviews;

    public IngestPauseDto pause() {
        return riotData.pause().map(IngestPauseDto::from).orElseGet(IngestPauseDto::unavailable);
    }

    public IngestPauseDto setPause(boolean paused) {
        return IngestPauseDto.from(riotData.setPause(paused));
    }

    public RiotDataInventoryDto inventory() {
        Optional<RiotDataGateway.Inventory> connecteur = riotData.inventory();
        return new RiotDataInventoryDto(connecteur.isPresent(),
                connecteur.map(inventaire -> somme(inventaire.purged())).orElse(0L), findings.count(),
                users.countByRiotPuuidNotNull(), placesSuivies(), teams.count(), reviews.count());
    }

    public RiotDataInvalidationDto invalidate(User actor, String confirmation) {
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
        return new RiotDataInvalidationDto(documents, constats, suivis.size());
    }

    private long placesSuivies() {
        return teams.findRiotPuuids().stream().flatMap(team -> team.getMembers().stream())
                .map(TeamMember::getRiotPuuid).filter(puuid -> puuid != null && !puuid.isBlank()).count();
    }

    private static long somme(Map<String, Long> parCollection) {
        return parCollection.values().stream().mapToLong(Long::longValue).sum();
    }
}
