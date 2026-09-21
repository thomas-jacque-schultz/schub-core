package schultz.thomas.schub.core.team.api.dto;

import schultz.thomas.schub.core.team.business.model.GameRole;
import schultz.thomas.schub.core.team.business.model.MemberStatus;
import schultz.thomas.schub.core.team.business.model.StatsState;

import java.util.List;

/** Une colonne du panneau « joueurs » : un membre de l'effectif, ses chiffres et leur assise. */
public record PlayerStatsDto(
        String memberId,
        String displayName,
        String avatarUrl,
        String riotGameName,
        String riotTagLine,
        MemberStatus status,
        GameRole role,
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
