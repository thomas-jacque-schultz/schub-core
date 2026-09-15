package schultz.thomas.schub.core.portforwarding;

/**
 * État du lien entre le connecteur et le routeur, tel que le connecteur le rapporte.
 *
 * @param unavailableReason motif empêchant d'agir, ou {@code null} si le routeur est utilisable
 */
public record RouterStatus(
        String router,
        boolean paired,
        String unavailableReason,
        String marker
) {
}
