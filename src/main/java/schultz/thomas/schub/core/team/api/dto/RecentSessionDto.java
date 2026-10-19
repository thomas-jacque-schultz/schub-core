package schultz.thomas.schub.core.team.api.dto;

import java.time.Instant;
import java.time.LocalDate;

public record RecentSessionDto(LocalDate day, Instant startedAt, String teamName, long games, long wins,
                               boolean planned) {
}
