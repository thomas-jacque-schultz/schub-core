package schultz.thomas.schub.core.lol.stats.business.model;

import java.util.List;

// Tout est dit depuis notre camp : « ours » vaut pour le gank de notre jungler, ou le côté de notre équipe.
public record EarlyGameView(List<Gank> ganks, JunglePresence ourJungler, JunglePresence theirJungler,
                           Objectives ourObjectives, Objectives theirObjectives) {

    public record Gank(int second, String lane, boolean ours, String outcome, boolean decisive,
                          boolean objectiveFollowUp, List<String> targetMemberIds,
                          List<String> fallenMemberIds, int alliesLost, int enemiesLost) {
    }

    public record JunglePresence(String memberId, int topMinutes, int midMinutes, int botMinutes,
                                    String strongSide) {
    }

    public record Objectives(int dragons, int grubs, int heralds) {
    }
}
