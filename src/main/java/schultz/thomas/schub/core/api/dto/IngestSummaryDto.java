package schultz.thomas.schub.core.api.dto;

import schultz.thomas.schub.core.business.service.RiotConnectorService.IngestSummary;

public record IngestSummaryDto(boolean available, Counts matches, Counts profiles) {

    public record Counts(long retrieved, long analysed, long pending) {

        static Counts from(IngestSummary.Counts c) {
            return new Counts(c.retrieved(), c.analysed(), c.pending());
        }
    }

    public static IngestSummaryDto from(IngestSummary summary) {
        return new IngestSummaryDto(true, Counts.from(summary.matches()), Counts.from(summary.profiles()));
    }

    public static IngestSummaryDto unavailable() {
        return new IngestSummaryDto(false, new Counts(0, 0, 0), new Counts(0, 0, 0));
    }
}
