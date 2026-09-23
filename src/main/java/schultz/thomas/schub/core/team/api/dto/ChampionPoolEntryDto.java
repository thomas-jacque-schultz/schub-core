package schultz.thomas.schub.core.team.api.dto;

import java.util.List;

public record ChampionPoolEntryDto(
        int championId,
        String championKey,
        String name,
        String iconUrl,
        List<ChampionPoolMemberDto> players,
        int setAsideByFloor
) {
}
