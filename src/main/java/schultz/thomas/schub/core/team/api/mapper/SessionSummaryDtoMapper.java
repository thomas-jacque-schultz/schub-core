package schultz.thomas.schub.core.team.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import schultz.thomas.schub.core.business.service.PremadeLabGateway;
import schultz.thomas.schub.core.team.api.dto.RecentSessionDto;
import schultz.thomas.schub.core.team.api.dto.SessionSummaryEmbedDto;
import schultz.thomas.schub.core.team.api.dto.SessionSummaryTestDto;
import schultz.thomas.schub.core.team.business.service.SessionSummaryService;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SessionSummaryDtoMapper {

    List<SessionSummaryEmbedDto> toEmbedDtos(List<PremadeLabGateway.Embed> embeds);

    List<RecentSessionDto> toRecentDtos(List<SessionSummaryService.RecentSession> sessions);

    SessionSummaryTestDto toDto(SessionSummaryService.TestResult result);
}
