package schultz.thomas.schub.core.team.api.dto;

import schultz.thomas.schub.core.team.business.model.GameRole;
import schultz.thomas.schub.core.team.business.model.MemberStatus;
import schultz.thomas.schub.core.team.business.model.StatsState;

import java.util.List;

public record PlayerStatsDto(
        String memberId,
        String displayName,
        String avatarUrl,
        String riotGameName,
        String riotTagLine,
        MemberStatus status,
        List<GameRole> roles,
        boolean linked,
        StatsState state,
        StatsCoverageDto coverage,
        StatLineDto overall,
        List<StatLineDto> champions,
        List<StatLineDto> positions,
        List<StatLineDto> queues,
        List<StatLineDto> months,
        List<RankedStandingDto> rankings,
        TeamComparisonDto versusTeammates
) {
}
