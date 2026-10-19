package schultz.thomas.schub.core.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import schultz.thomas.schub.core.api.dto.PortForwardingReportDto;
import schultz.thomas.schub.core.api.dto.PortRuleDto;
import schultz.thomas.schub.core.api.dto.PortainerStackDto;
import schultz.thomas.schub.core.business.model.PortForwardingReport;
import schultz.thomas.schub.core.business.model.PortRule;
import schultz.thomas.schub.core.business.model.PortainerStack;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface PortForwardingDtoMapper {

    List<PortRuleDto> toRuleDtos(List<PortRule> rules);

    PortForwardingReportDto toDto(PortForwardingReport report);

    List<PortainerStackDto> toStackDtos(List<PortainerStack> stacks);
}
