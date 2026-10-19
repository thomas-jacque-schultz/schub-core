package schultz.thomas.schub.core.lol.team.api.dto;

import schultz.thomas.schub.core.lol.team.data.model.GameRole;

import java.util.List;

public record ChampionPoolColumnDto(
        GameRole role,
        List<ChampionPoolEntryDto> champions,
        List<ChampionPoolMemberDto> unavailableMembers,
        int hiddenByFloor
) {
}
