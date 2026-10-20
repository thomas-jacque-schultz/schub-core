package schultz.thomas.schub.core.lol.stats.api.dto;

import jakarta.annotation.Nullable;

public record SideRanksDto(@Nullable AverageRankDto solo, @Nullable AverageRankDto flex) {
}
