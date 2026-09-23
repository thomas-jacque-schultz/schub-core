package schultz.thomas.schub.core.team.business.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface RiotChampionGateway {

    Optional<Catalogue> catalogue();

    Optional<List<Mastery>> masteries(String puuid, int limit);

    default Optional<List<Mastery>> masteries(String puuid) {
        return masteries(puuid, 0);
    }

    record Catalogue(String version, Map<Integer, Champion> parId) {
    }

    record Champion(int id, String key, String name, String iconUrl) {
    }

    record Mastery(int championId, int level, int points, Instant lastPlayedAt, Instant observedAt) {
    }
}
