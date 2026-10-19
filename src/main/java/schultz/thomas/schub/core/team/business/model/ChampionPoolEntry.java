package schultz.thomas.schub.core.team.business.model;

import java.util.List;

public record ChampionPoolEntry(
        int championId,
        String championKey,
        String name,
        String iconUrl,
        List<ChampionPoolMember> players,
        int setAsideByFloor
) {
}
