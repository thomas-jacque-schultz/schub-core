package schultz.thomas.schub.core.lol.seo.api.dto;

import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.List;

public record PlayerSuggestionDto(String riotId, String gameName, String tagLine, long matchCount,
                                  List<String> positions, @Nullable Instant lastPlayedAt) {
}
