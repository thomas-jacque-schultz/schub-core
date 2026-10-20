package schultz.thomas.schub.core.lol.team.api.dto;

import jakarta.annotation.Nullable;
import java.util.List;

public record ChampionPoolEntryDto(
        int championId,
        String championKey,
        @Nullable String name,
        @Nullable String iconUrl,
        List<ChampionPoolMemberDto> players,
        int setAsideByFloor
) {
}
