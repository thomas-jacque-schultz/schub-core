package schultz.thomas.schub.core.lol.stats.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import schultz.thomas.schub.core.lol.stats.business.model.StatsState;
import schultz.thomas.schub.core.lol.team.data.model.GameRole;
import schultz.thomas.schub.core.lol.team.data.model.MemberStatus;

import jakarta.annotation.Nullable;
import java.util.List;

public record PlayerStatsDto(
        String memberId,
        @Nullable String displayName,
        @Nullable String avatarUrl,
        @Nullable String riotGameName,
        @Nullable String riotTagLine,
        MemberStatus status,
        List<GameRole> roles,
        boolean linked,
        StatsState state,
        @Nullable StatsCoverageDto coverage,
        @Nullable StatLineDto overall,
        List<StatLineDto> champions,
        List<StatLineDto> positions,
        List<StatLineDto> queues,
        List<StatLineDto> months,
        List<RankedStandingDto> rankings,
        @Nullable RadarReferencesDto references,
        @Schema(description = "Sur les parties d'équipe de la période seulement. Absente : aucune partie d'équipe.")
        @Nullable StatLineDto premade
) {
}
