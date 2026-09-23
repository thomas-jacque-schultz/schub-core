package schultz.thomas.schub.core.business.service;

import java.util.Optional;

public interface RiotIdResolver {

    RiotIdResolution resolve(String gameName, String tagLine);

    default Optional<String> resolvePuuid(String gameName, String tagLine) {
        return Optional.ofNullable(resolve(gameName, tagLine).puuid());
    }
}
