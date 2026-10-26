package schultz.thomas.schub.core.lol.stats.business.model;

import java.util.List;

// Tout est dit depuis notre camp : « ours » vaut pour le gank de notre jungler, ou le côté de notre équipe.
public record EarlyGameView(List<Gank> ganks, JunglePresence ourJungler, JunglePresence theirJungler,
                           Objectives ourObjectives, Objectives theirObjectives, SupportLine ourSupport,
                           SupportLine theirSupport, JunglerLine ourJunglerMate, JunglerLine theirJunglerMate) {

    public record Gank(int second, String lane, boolean ours, String outcome, boolean decisive,
                          boolean objectiveFollowUp, List<String> targetMemberIds,
                          List<String> fallenMemberIds, int alliesLost, int enemiesLost) {
    }

    public record JunglePresence(String memberId, int topMinutes, int midMinutes, int botMinutes,
                                    String strongSide) {
    }

    public record Objectives(int dragons, int grubs, int heralds) {
    }

    // Schub#152 : parts des images à moins de 2 000 unités ; absentes sous 5 images comptées.
    public record SupportLine(String memberId, int championId, Double adcEarlyShare, Integer roams,
                              Double junglerLateShare) {
    }

    // Le poste dont le jungler a été le plus proche sur toute la partie.
    public record JunglerLine(String memberId, int championId, String position, double share) {
    }
}
