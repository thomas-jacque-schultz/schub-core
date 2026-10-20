package schultz.thomas.schub.core.lol.session.api.dto;

import schultz.thomas.schub.core.business.service.PremadeLabGateway;

import jakarta.annotation.Nullable;
import java.util.List;

public record SessionSummaryTestDto(@Nullable List<SessionSummaryEmbedDto> embeds,
                                    @Nullable PremadeLabGateway.Status status, @Nullable String detail) {
}
