package schultz.thomas.schub.core.team.api.dto;

import java.time.Instant;
import java.util.List;

public record PlayerSuggestionDto(String riotId, String gameName, String tagLine, long matchCount,
                                  List<String> positions, Instant lastPlayedAt) {
}
