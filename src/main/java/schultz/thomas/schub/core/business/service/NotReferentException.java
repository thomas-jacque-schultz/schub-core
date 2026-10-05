package schultz.thomas.schub.core.business.service;

import org.springframework.security.access.AccessDeniedException;

public class NotReferentException extends AccessDeniedException {

    public NotReferentException(String slug) {
        super("Tu n'es pas référent du serveur " + slug);
    }
}
