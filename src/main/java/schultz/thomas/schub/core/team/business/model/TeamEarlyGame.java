package schultz.thomas.schub.core.team.business.model;

import java.util.List;

public record TeamEarlyGame(int games, List<MemberEarly> members, List<StrongSide> strongSides) {

    // Laner : ganks subis et tenus. Jungler : ganks faits, décisifs, et le côté où il a joué.
    public record MemberEarly(String memberId, String displayName, int laneGames, int ganksFaced,
                                 int ganksHeld, int deathsOnGank, int jungleGames, int ganksMade,
                                 int ganksDecisive, int ganksCountered, int topMinutes, int midMinutes,
                                 int botMinutes, List<PresenceWith> presenceWith) {
    }

    // Minutes passées par ce jungler du côté où jouait ce coéquipier, sur leurs parties communes.
    public record PresenceWith(String memberId, String displayName, int games, int minutes, int totalMinutes) {
    }

    // Côté fort = côté où notre jungler a passé le plus de temps ; les ganks adverses le fuient-ils ?
    public record StrongSide(String side, int games, int wins, int enemyGanks, int enemyGanksOnWeakSide) {
    }
}
