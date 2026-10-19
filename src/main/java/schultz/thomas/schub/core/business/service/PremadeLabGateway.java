package schultz.thomas.schub.core.business.service;

import java.util.List;
import java.util.Map;

// Les messages privés de l'application Discord PremadeLab (Schub#100).
public interface PremadeLabGateway {

    Result sendDirectMessage(String discordId, List<Embed> embeds);

    // La forme <:Gnar:123> de chaque champion ; un champion absent s'écrit en toutes lettres.
    Map<String, String> championEmojis(List<ChampionIcon> champions);

    record ChampionIcon(String key, String iconUrl) {
    }

    enum Status {
        SENT,
        UNREACHABLE,
        DISABLED,
        FAILED
    }

    record Result(Status status, String detail) {
    }

    // Le format des encadrés de l’API Discord.
    record Embed(String title, String url, String description, Integer color, List<Field> fields, Footer footer) {

        public record Field(String name, String value, boolean inline) {
        }

        public record Footer(String text) {
        }
    }
}
