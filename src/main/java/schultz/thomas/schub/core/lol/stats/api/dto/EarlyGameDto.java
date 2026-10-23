package schultz.thomas.schub.core.lol.stats.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.annotation.Nullable;
import java.util.List;

// Tout est dit depuis notre camp : « ours » vaut pour le gank de notre jungler, ou le côté de notre équipe.
public record EarlyGameDto(List<GankDto> ganks, @Nullable JunglePresenceDto ourJungler,
                           @Nullable JunglePresenceDto theirJungler, ObjectivesDto ourObjectives,
                           ObjectivesDto theirObjectives, @Nullable SupportProximityDto ourSupport,
                           @Nullable SupportProximityDto theirSupport,
                           @Nullable JunglerProximityDto ourJunglerClosest,
                           @Nullable JunglerProximityDto theirJunglerClosest) {

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

    @Schema(description = "Parts en pourcentage des minutes comptées : à moins de 2 000 unités, sans fontaine ni mort "
            + "récente. Null sous 5 minutes comptées.")
    public record SupportProximityDto(@Nullable Integer adcShareEarly, int adcMinutesEarly,
                                      @Schema(description = "Départs d'auprès de l'ADC avant 15 min.") int roams,
                                      @Nullable Integer junglerShareLate, int junglerMinutesLate) {
    }

    @Schema(description = "Le coéquipier dont le jungler a été le plus souvent proche, sur toute la partie.")
    public record JunglerProximityDto(String position, int share, int minutes) {
    }
}
