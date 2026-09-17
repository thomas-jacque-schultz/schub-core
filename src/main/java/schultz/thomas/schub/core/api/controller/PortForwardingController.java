package schultz.thomas.schub.core.api.controller;

import schultz.thomas.schub.core.api.dto.PortForwardingReport;
import schultz.thomas.schub.core.api.dto.PortRule;
import schultz.thomas.schub.core.business.service.PortForwardingService;
import schultz.thomas.schub.core.business.service.RedirectionRequestService;
import schultz.thomas.schub.core.business.service.StaticPortRuleService;
import schultz.thomas.schub.core.config.PortForwardingProperties;
import schultz.thomas.schub.core.data.model.StaticPortRuleEntity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

/**
 * Inspection et pilotage des redirections de ports.
 * Protégé comme le reste de l'API par le filtre de secret interne.
 *
 * <p>Deux lectures complémentaires, à ne pas confondre :</p>
 * <ul>
 *   <li>{@code /rules} montre l'état <em>réel du routeur</em>, redirections manuelles comprises ;</li>
 *   <li>{@code /static-rules} montre les règles permanentes <em>que cette application détient</em>,
 *       donc celles qu'elle peut supprimer.</li>
 * </ul>
 *
 * <p>Une règle posée pour un serveur de jeu n'apparaît que dans la première : elle est dérivée
 * du serveur et se supprime en modifiant le serveur, jamais directement.</p>
 */
@Slf4j
@RestController
@RequestMapping("/port-forwarding")
@RequiredArgsConstructor
public class PortForwardingController {

    private final PortForwardingService portForwardingService;

    private final RedirectionRequestService redirectionRequestService;

    private final PortForwardingProperties portForwardingProperties;

    private final StaticPortRuleService staticPortRuleService;

    /** État courant tel que le routeur le rapporte, redirections manuelles comprises. */
    @GetMapping("/rules")
    public ResponseEntity<List<PortRule>> listRules() {
        if (!portForwardingProperties.isEnabled()) {
            return ResponseEntity.status(503).build();
        }
        return ResponseEntity.ok(redirectionRequestService.listRules());
    }

    /** Les règles permanentes détenues par l'application — celles qui portent un bouton supprimer. */
    @GetMapping("/static-rules")
    public List<StaticPortRuleEntity> listStaticRules() {
        return staticPortRuleService.findAll();
    }

    /**
     * Ajoute une règle permanente, puis réaligne le routeur sans attendre le scheduler :
     * un ajout qui ne se voit qu'à la minute suivante passerait pour un échec.
     */
    @PostMapping("/static-rules")
    public ResponseEntity<StaticPortRuleEntity> createStaticRule(@RequestBody StaticPortRuleEntity rule) {
        StaticPortRuleEntity created = staticPortRuleService.create(rule);
        reconcileQuietly("ajout de " + created.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/static-rules/{id}")
    public ResponseEntity<Void> deleteStaticRule(@PathVariable String id) {
        if (!staticPortRuleService.delete(id)) {
            return ResponseEntity.notFound().build();
        }
        reconcileQuietly("suppression de la règle " + id);
        return ResponseEntity.noContent().build();
    }

    /** Force une réconciliation immédiate et renvoie le détail de ce qui a été fait. */
    @PostMapping("/reconcile")
    public PortForwardingReport reconcile() {
        return portForwardingService.reconcile();
    }

    /** État de l'intégration, pour diagnostiquer sans lire les logs. */
    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of(
                "enabled", portForwardingProperties.isEnabled(),
                "dryRun", portForwardingProperties.isDryRun(),
                "unavailableReason", String.valueOf(redirectionRequestService.unavailableReason()),
                "defaultLanIp", portForwardingProperties.getDefaultLanIp(),
                "pruneOrphans", portForwardingProperties.isPruneOrphans(),
                "staticRules", staticPortRuleService.findAll().size()
        );
    }

    /**
     * La règle est déjà enregistrée : si le routeur est injoignable, l'écriture reste valide et
     * la boucle de réconciliation rattrapera. Échouer ici effacerait un enregistrement correct.
     */
    private void reconcileQuietly(String cause) {
        try {
            portForwardingService.reconcile();
        } catch (RuntimeException e) {
            log.warn("Réconciliation immédiate impossible après {} — la boucle périodique rattrapera: {}",
                    cause, e.getMessage());
        }
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleInvalid(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleConflict(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
    }
}
