package schultz.thomas.schub.core.lol.stats.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.annotation.Nullable;
import java.util.List;

// Tout est dit depuis notre camp : « ours » vaut pour le gank de notre jungler, ou le côté de notre équipe.
public record EarlyGameDto(List<GankDto> ganks, @Nullable JunglePresenceDto ourJungler,
                           @Nullable JunglePresenceDto theirJungler, ObjectivesDto ourObjectives,
                           ObjectivesDto theirObjectives, @Nullable SupportLineDto ourSupport,
                           @Nullable SupportLineDto theirSupport, @Nullable JunglerLineDto ourJunglerMate,
                           @Nullable JunglerLineDto theirJunglerMate) {

    public record GankDto(int second, @Schema(allowableValues = {"TOP", "MID", "BOT"}) String lane, boolean ours,
                          @Schema(allowableValues = {"KILL", "TRADE", "SURVIVED", "COUNTER"}) String outcome,
                          boolean decisive, boolean objectiveFollowUp, List<String> targetMemberIds,
                          List<String> fallenMemberIds, int alliesLost, int enemiesLost) {
    }

    public record JunglePresenceDto(@Nullable String memberId, int topMinutes, int midMinutes, int botMinutes,
                                    @Schema(allowableValues = {"TOP", "BOT", "BALANCED"}) String strongSide) {
    }

    public record ObjectivesDto(int dragons, int grubs, int heralds) {
    }

    @Schema(description = """
            Le support avec son ADC avant 15 min (part des images à moins de 2 000 unités, et départs) et avec son
            jungler après. Une part est absente sous 5 images comptées.""")
    public record SupportLineDto(@Nullable String memberId, int championId, @Nullable Double adcEarlyShare,
                                 @Nullable Integer roams, @Nullable Double junglerLateShare) {
    }

    @Schema(description = "Le poste dont le jungler a été le plus proche sur toute la partie, et la part des images.")
    public record JunglerLineDto(@Nullable String memberId, int championId,
                                 @Schema(allowableValues = {"TOP", "MIDDLE", "BOTTOM", "UTILITY"}) String position,
                                 double share) {
    }
}
