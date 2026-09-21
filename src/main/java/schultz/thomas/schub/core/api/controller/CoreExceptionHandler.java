package schultz.thomas.schub.core.api.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import schultz.thomas.schub.core.business.service.RiotAccountChangeNotConfirmedException;
import schultz.thomas.schub.core.business.service.RiotConnectorUnavailableException;
import schultz.thomas.schub.core.business.service.UnknownRiotAccountException;

import java.util.Map;
import java.util.NoSuchElementException;

/** Traduit les refus du domaine en statuts HTTP qui portent leur motif. */
@Slf4j
@RestControllerAdvice
public class CoreExceptionHandler {

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleMissing(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }

    /**
     * 403, avec le motif.
     *
     * <p>Volontairement pas 404 : masquer l'existence de la ressource n'apporte rien ici — la
     * frontière du maillage est déjà fermée par le secret interne, et seul un service Schub
     * atteint ce point. Un 403 explicite est ce qui permet au BFF de distinguer « droits
     * insuffisants » de « n'existe pas », donc d'afficher le bon message.</p>
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleDenied(AccessDeniedException e) {
        log.warn("Accès refusé : {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleInvalid(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    /**
     * 409 avec les conséquences dans le corps, et pas seulement un message.
     *
     * <p>Un refus qui dit « confirmez » sans dire ce qu'on confirme oblige l'écran à
     * réécrire les conséquences en dur, donc à diverger du serveur au premier changement de
     * règle. {@code change} les porte, et l'écran n'a qu'à les afficher.</p>
     */
    @ExceptionHandler(RiotAccountChangeNotConfirmedException.class)
    public ResponseEntity<Map<String, Object>> handleUnconfirmedChange(
            RiotAccountChangeNotConfirmedException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", e.getMessage(), "change", e.getChange()));
    }

    @ExceptionHandler(UnknownRiotAccountException.class)
    public ResponseEntity<Map<String, String>> handleUnknownRiotAccount(UnknownRiotAccountException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(RiotConnectorUnavailableException.class)
    public ResponseEntity<Map<String, String>> handleConnectorDown(RiotConnectorUnavailableException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleConflict(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
    }
}
