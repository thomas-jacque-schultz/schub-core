package schultz.thomas.schub.core.business.service;

import java.util.Optional;

public interface RiotIdResolver {

    RiotIdResolution resolve(String gameName, String tagLine);

    // Accepte une résolution de moins d'une heure, sans appel à Riot : pour une page rafraîchie souvent.
    default RiotIdResolution resolveRecent(String gameName, String tagLine) {
        return resolve(gameName, tagLine);
    }

    default Optional<String> resolvePuuid(String gameName, String tagLine) {
        return Optional.ofNullable(resolve(gameName, tagLine).puuid());
    }
}
