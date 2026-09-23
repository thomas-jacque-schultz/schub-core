package schultz.thomas.schub.core.api.controller;

import schultz.thomas.schub.core.api.dto.PortForwardingReport;
import schultz.thomas.schub.core.api.dto.PortRule;
import schultz.thomas.schub.core.api.dto.StaticPortRuleDto;
import schultz.thomas.schub.core.api.dto.StaticPortRuleRequest;
import schultz.thomas.schub.core.business.mapper.StaticPortRuleMapper;
import schultz.thomas.schub.core.business.model.Permission;
import schultz.thomas.schub.core.business.service.PermissionEvaluator;
import schultz.thomas.schub.core.business.service.PortForwardingService;
import schultz.thomas.schub.core.business.service.RedirectionRequestService;
import schultz.thomas.schub.core.business.service.StaticPortRuleService;
import schultz.thomas.schub.core.business.service.UserService;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/port-forwarding")
@RequiredArgsConstructor
public class PortForwardingController {

    private final PortForwardingService portForwardingService;

    private final RedirectionRequestService redirectionRequestService;

    private final PortForwardingProperties portForwardingProperties;

    private final StaticPortRuleService staticPortRuleService;
    private final StaticPortRuleMapper staticPortRuleMapper;

    private final PermissionEvaluator permissionEvaluator;

    private final UserService userService;

    @GetMapping("/rules")
    public ResponseEntity<List<PortRule>> listRules(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId) {
        require(actorDiscordId, Permission.PORT_VIEW);
        if (!portForwardingProperties.isEnabled()) {
            return ResponseEntity.status(503).build();
        }
        return ResponseEntity.ok(redirectionRequestService.listRules());
    }

    @GetMapping("/static-rules")
    public List<StaticPortRuleDto> listStaticRules(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId) {
        require(actorDiscordId, Permission.PORT_VIEW);
        return staticPortRuleMapper.toDtos(staticPortRuleService.findAll());
    }

    @PostMapping("/static-rules")
    public ResponseEntity<StaticPortRuleDto> createStaticRule(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId,
            @RequestBody StaticPortRuleRequest request) {
        require(actorDiscordId, Permission.PORT_RULE_EDIT);
        StaticPortRuleEntity created = staticPortRuleService.create(staticPortRuleMapper.toEntity(request));
        reconcileQuietly("ajout de " + created.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(staticPortRuleMapper.toDto(created));
    }

    @DeleteMapping("/static-rules/{id}")
    public ResponseEntity<Void> deleteStaticRule(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId,
            @PathVariable String id) {
        require(actorDiscordId, Permission.PORT_RULE_EDIT);
        if (!staticPortRuleService.delete(id)) {
            return ResponseEntity.notFound().build();
        }
        reconcileQuietly("suppression de la règle " + id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reconcile")
    public PortForwardingReport reconcile(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId) {
        require(actorDiscordId, Permission.PORT_RULE_EDIT);
        return portForwardingService.reconcile();
    }

    @GetMapping("/status")
    public Map<String, Object> status(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId) {
        require(actorDiscordId, Permission.PORT_VIEW);
        return Map.of(
                "enabled", portForwardingProperties.isEnabled(),
                "dryRun", portForwardingProperties.isDryRun(),
                "unavailableReason", String.valueOf(redirectionRequestService.unavailableReason()),
                "defaultLanIp", portForwardingProperties.getDefaultLanIp(),
                "pruneOrphans", portForwardingProperties.isPruneOrphans(),
                "staticRules", staticPortRuleService.findAll().size()
        );
    }

    private void require(String actorDiscordId, Permission permission) {
        permissionEvaluator.require(userService.requireActor(actorDiscordId), permission, null);
    }

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
