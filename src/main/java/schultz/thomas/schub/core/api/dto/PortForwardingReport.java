package schultz.thomas.schub.core.api.dto;

import java.util.List;

/**
 * Compte rendu d'une réconciliation des redirections de ports.
 *
 * @param applied       faux quand rien n'a été tenté (intégration coupée, routeur injoignable)
 * @param skippedReason motif dans ce cas, null sinon
 * @param outcomes      ce qui a été décidé pour chaque règle voulue ou orpheline
 * @param conflicts     règles voulues dont le port est déjà pris par une redirection manuelle
 * @param rejected      règles écartées avant même d'interroger le routeur
 */
public record PortForwardingReport(
        boolean applied,
        String skippedReason,
        List<RuleOutcome> outcomes,
        List<String> conflicts,
        List<String> rejected
) {

    /** Ce qu'il a fallu faire d'une règle. */
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

    /** Décompte par type, pour une ligne de log lisible. */
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
