package schultz.thomas.schub.core.team.api.dto;

import java.util.List;

public record TeamEarlyGameDto(int games, List<MemberEarlyDto> members, List<StrongSideDto> strongSides) {

    // Laner : ganks subis et tenus. Jungler : ganks faits, décisifs, et le côté où il a joué.
    public record MemberEarlyDto(String memberId, String displayName, int laneGames, int ganksFaced,
                                 int ganksHeld, int deathsOnGank, int jungleGames, int ganksMade,
                                 int ganksDecisive, int ganksCountered, int topMinutes, int midMinutes,
                                 int botMinutes) {
    }

    // Côté fort = côté où notre jungler a passé le plus de temps ; les ganks adverses le fuient-ils ?
    public record StrongSideDto(String side, int games, int wins, int enemyGanks, int enemyGanksOnWeakSide) {
    }
}
