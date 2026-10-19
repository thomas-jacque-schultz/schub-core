package schultz.thomas.schub.core.team.business.model;

import schultz.thomas.schub.core.team.data.model.GameRole;

import java.util.List;

public record ChampionPoolColumn(
        GameRole role,
        List<ChampionPoolEntry> champions,
        List<ChampionPoolMember> unavailableMembers,
        int hiddenByFloor
) {
}
