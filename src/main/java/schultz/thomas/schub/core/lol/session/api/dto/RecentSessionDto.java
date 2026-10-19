package schultz.thomas.schub.core.lol.session.api.dto;

import java.time.Instant;
import java.time.LocalDate;

public record RecentSessionDto(LocalDate day, Instant startedAt, String teamName, long games, long wins,
                               boolean planned) {
}
