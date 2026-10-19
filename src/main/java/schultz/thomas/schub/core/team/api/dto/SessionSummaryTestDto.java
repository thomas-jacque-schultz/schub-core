package schultz.thomas.schub.core.team.api.dto;

import schultz.thomas.schub.core.business.service.PremadeLabGateway;

import java.util.List;

public record SessionSummaryTestDto(List<SessionSummaryEmbedDto> embeds, PremadeLabGateway.Status status,
                                    String detail) {
}
