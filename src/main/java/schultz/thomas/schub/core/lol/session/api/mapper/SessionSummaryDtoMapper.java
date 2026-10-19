package schultz.thomas.schub.core.lol.session.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import schultz.thomas.schub.core.business.service.PremadeLabGateway;
import schultz.thomas.schub.core.lol.session.api.dto.RecentSessionDto;
import schultz.thomas.schub.core.lol.session.api.dto.SessionSummaryEmbedDto;
import schultz.thomas.schub.core.lol.session.api.dto.SessionSummaryTestDto;
import schultz.thomas.schub.core.lol.session.api.dto.TeamSessionDto;
import schultz.thomas.schub.core.lol.session.business.service.SessionSummaryService;
import schultz.thomas.schub.core.lol.session.data.model.TeamSession;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SessionSummaryDtoMapper {

    List<SessionSummaryEmbedDto> toEmbedDtos(List<PremadeLabGateway.Embed> embeds);

    List<RecentSessionDto> toRecentDtos(List<SessionSummaryService.RecentSession> sessions);

    SessionSummaryTestDto toDto(SessionSummaryService.TestResult result);

    List<TeamSessionDto> toSessionDtos(List<TeamSession> sessions);
}
