package schultz.thomas.schub.core.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import schultz.thomas.schub.core.api.dto.HistoryWindowDto;
import schultz.thomas.schub.core.business.service.RiotConnectorService;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface IngestDtoMapper {

    HistoryWindowDto toDto(RiotConnectorService.HistoryWindow window);

    RiotConnectorService.HistoryWindow fromDto(HistoryWindowDto window);
}
