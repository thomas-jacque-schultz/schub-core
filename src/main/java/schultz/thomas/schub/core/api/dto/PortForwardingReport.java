package schultz.thomas.schub.core.api.dto;

import java.util.List;

public record PortForwardingReport(
        boolean applied,
        String skippedReason,
        List<RuleOutcome> outcomes,
        List<String> conflicts,
        List<String> rejected
) {

    public enum Outcome {
        CREATED,
        OPENED,
        CLOSED,
        REROUTED,
        DELETED,
        UNCHANGED
    }

    public record RuleOutcome(Outcome outcome, String rule) {
    }

    public static PortForwardingReport skipped(String reason) {
        return new PortForwardingReport(false, reason, List.of(), List.of(), List.of());
    }

    public boolean hasChanges() {
        return outcomes.stream().anyMatch(entry -> entry.outcome() != Outcome.UNCHANGED);
    }

    public long count(Outcome outcome) {
        return outcomes.stream().filter(entry -> entry.outcome() == outcome).count();
    }

    public String summary() {
        StringBuilder summary = new StringBuilder();
        for (Outcome outcome : Outcome.values()) {
            long count = count(outcome);
            if (count > 0) {
                if (!summary.isEmpty()) {
                    summary.append(", ");
                }
                summary.append(count).append(' ').append(outcome.name().toLowerCase());
            }
        }
        return summary.isEmpty() ? "rien à faire" : summary.toString();
    }
}
