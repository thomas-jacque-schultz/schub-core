package schultz.thomas.schub.core.team.api.dto;

import schultz.thomas.schub.core.team.business.model.GameRole;

import java.util.List;

public record ChampionPoolColumnDto(
        GameRole role,
        List<ChampionPoolEntryDto> champions,
        List<ChampionPoolMemberDto> unavailableMembers,
        int hiddenByFloor
) {
}
