package schultz.thomas.schub.core.team.api.dto;

import java.util.List;

// Tout est dit depuis notre camp : « ours » vaut pour le gank de notre jungler, ou le côté de notre équipe.
public record EarlyGameDto(List<GankDto> ganks, JunglePresenceDto ourJungler, JunglePresenceDto theirJungler,
                           ObjectivesDto ourObjectives, ObjectivesDto theirObjectives) {

    public record GankDto(int second, String lane, boolean ours, String outcome, boolean decisive,
                          boolean objectiveFollowUp, List<String> targetMemberIds,
                          List<String> fallenMemberIds, int alliesLost, int enemiesLost) {
    }

    public record JunglePresenceDto(String memberId, int topMinutes, int midMinutes, int botMinutes,
                                    String strongSide) {
    }

    public record ObjectivesDto(int dragons, int grubs, int heralds) {
    }
}
