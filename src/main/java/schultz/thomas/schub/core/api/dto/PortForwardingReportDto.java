package schultz.thomas.schub.core.api.dto;

import schultz.thomas.schub.core.business.model.PortForwardingReport;

import java.util.List;

public record PortForwardingReportDto(
        boolean applied,
        String skippedReason,
        List<RuleOutcomeDto> outcomes,
        List<String> conflicts,
        List<String> rejected
) {

    public record RuleOutcomeDto(PortForwardingReport.Outcome outcome, String rule) {
    }
}
