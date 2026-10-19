package schultz.thomas.schub.core.lol.stats.business.model;

import io.swagger.v3.oas.annotations.media.Schema;
import schultz.thomas.schub.core.lol.team.data.model.GameRole;
import schultz.thomas.schub.core.lol.team.data.model.MemberStatus;

import java.util.List;

public record PlayerStats(
        String memberId,
        String displayName,
        String avatarUrl,
        String riotGameName,
        String riotTagLine,
        MemberStatus status,
        List<GameRole> roles,
        boolean linked,
        StatsState state,
        StatsCoverage coverage,
        StatLine overall,
        List<StatLine> champions,
        List<StatLine> positions,
        List<StatLine> queues,
        List<StatLine> months,
        List<RankedStanding> rankings,
        RadarReferences references,
        @Schema(description = "Sur les parties d'équipe de la période seulement. Absente : aucune partie d'équipe.")
        StatLine premade
) {
}
