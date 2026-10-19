package schultz.thomas.schub.core.lol.riot.business.model;

import java.util.Map;
import java.util.Optional;

// Les modes tels que le connecteur Riot les nomme. Une file qu'il ne connaît pas (1750, 9 contre 9) arrive en OTHER.
public final class GameModes {

    public static final String AUTRE = "OTHER";

    private static final Map<String, String> HORS_FAILLE = Map.ofEntries(Map.entry("ARAM", "ARAM"),
            Map.entry("ARAM_MAYHEM", "ARAM Mayhem"), Map.entry("ARAM_CLASH", "Clash ARAM"),
            Map.entry("ARENA", "Arène"), Map.entry("URF", "URF"), Map.entry("ARURF", "ARURF"),
            Map.entry("ONE_FOR_ALL", "Un pour tous"), Map.entry("NEXUS_BLITZ", "Nexus Blitz"),
            Map.entry("ULTIMATE_SPELLBOOK", "Grimoire ultime"), Map.entry("BRAWL", "Brawl"),
            Map.entry("SWARM", "Swarm"), Map.entry("COOP_VS_AI", "Contre l'IA"));

    private GameModes() {
    }

    public static String de(String queue) {
        return queue == null || queue.isBlank() ? AUTRE : queue;
    }

    public static Optional<String> libelleHorsFaille(String queue) {
        return Optional.ofNullable(queue).map(HORS_FAILLE::get);
    }
}
