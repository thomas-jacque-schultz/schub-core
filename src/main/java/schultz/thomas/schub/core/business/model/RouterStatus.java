package schultz.thomas.schub.core.business.model;

public record RouterStatus(
        String router,
        boolean paired,
        String unavailableReason,
        String marker
) {
}
