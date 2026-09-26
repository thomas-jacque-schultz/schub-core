package schultz.thomas.schub.core.business.service;

import schultz.thomas.schub.core.api.dto.PortForwardingReport.Outcome;
import schultz.thomas.schub.core.api.dto.PortForwardingReport.RuleOutcome;
import schultz.thomas.schub.core.api.dto.PortForwardingReport;
import schultz.thomas.schub.core.api.dto.PortRule;
import schultz.thomas.schub.core.api.dto.PortRuleKey;
import schultz.thomas.schub.core.business.model.PortRuleResolution;
import schultz.thomas.schub.core.config.PortForwardingProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortForwardingService {

    private final PortForwardingProperties properties;

    private final RedirectionRequestService redirectionRequestService;

    private final PortRuleResolver resolver;

    private final ReentrantLock lock = new ReentrantLock();

    private boolean failing;

    public PortForwardingReport reconcile() {
        return reconcile(null, false);
    }

    public PortForwardingReport reconcile(String overrideIdentifier, boolean overrideOpen) {
        String inactiveReason = inactiveReason();
        if (inactiveReason != null) {
            log.debug("Réconciliation des ports ignorée: {}", inactiveReason);
            return PortForwardingReport.skipped(inactiveReason);
        }

        lock.lock();
        try {
            PortForwardingReport report = doReconcile(overrideIdentifier, overrideOpen);
            if (failing) {
                failing = false;
                log.info("Réconciliation des redirections rétablie");
            }
            return report;
        } catch (Exception e) {
            if (failing) {
                log.warn("Réconciliation des redirections toujours en échec: {}", e.getMessage());
            } else {
                failing = true;
                log.error("Réconciliation des redirections en échec", e);
            }
            return PortForwardingReport.skipped("erreur: " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    private String inactiveReason() {
        if (!properties.isEnabled()) {
            return "port-forwarding.enabled=false";
        }
        return redirectionRequestService.unavailableReason();
    }

    private PortForwardingReport doReconcile(String overrideIdentifier, boolean overrideOpen) {
        PortRuleResolution desired = resolver.resolve(overrideIdentifier, overrideOpen);
        Plan plan = plan(desired.rules(), redirectionRequestService.listRules());
        PortForwardingReport report = execute(plan, desired.rejected());

        if (report.hasChanges()) {
            log.info("Redirections synchronisées: {}", report.summary());
        }
        if (!report.conflicts().isEmpty()) {
            log.warn("Redirections manuelles en conflit, laissées intactes: {}", report.conflicts());
        }
        if (!report.rejected().isEmpty()) {
            log.warn("Règles écartées: {}", report.rejected());
        }
        return report;
    }

    private Plan plan(Map<PortRuleKey, PortRule> desired, List<PortRule> current) {
        List<PlannedAction> actions = new ArrayList<>();
        List<String> conflicts = new ArrayList<>();

        Map<PortRuleKey, PortRule> currentByKey = new LinkedHashMap<>();
        current.forEach(rule -> currentByKey.putIfAbsent(rule.key(), rule));

        desired.forEach((key, wanted) -> {
            PortRule existing = currentByKey.get(key);
            if (existing == null) {
                actions.add(new PlannedAction(Outcome.CREATED, wanted));
            } else if (!existing.managed()) {
                conflicts.add(wanted.describe() + " occupé par une redirection manuelle");
            } else {
                actions.add(decide(existing, wanted));
            }
        });

        current.stream()
                .filter(rule -> rule.managed() && !desired.containsKey(rule.key()))
                .map(this::decideOrphan)
                .filter(Objects::nonNull)
                .forEach(actions::add);

        return new Plan(actions, conflicts);
    }

    private PlannedAction decide(PortRule existing, PortRule wanted) {
        PortRule target = wanted.withProviderId(existing.providerId());

        if (!existing.hasSameRoutingAs(wanted)) {
            return new PlannedAction(Outcome.REROUTED, target);
        }
        if (existing.open() != wanted.open()) {
            return new PlannedAction(wanted.open() ? Outcome.OPENED : Outcome.CLOSED, target);
        }
        return new PlannedAction(Outcome.UNCHANGED, target);
    }

    private PlannedAction decideOrphan(PortRule orphan) {
        if (properties.isPruneOrphans()) {
            return new PlannedAction(Outcome.DELETED, orphan);
        }
        return orphan.open() ? new PlannedAction(Outcome.CLOSED, orphan.withOpen(false)) : null;
    }

    private PortForwardingReport execute(Plan plan, List<String> rejected) {
        List<RuleOutcome> outcomes = new ArrayList<>();

        for (PlannedAction action : plan.actions()) {
            if (action.outcome() != Outcome.UNCHANGED) {
                if (properties.isDryRun()) {
                    log.info("[dry-run] {} : {}", action.outcome(), describe(action.rule()));
                } else {
                    apply(action);
                }
            }
            outcomes.add(new RuleOutcome(action.outcome(), describe(action.rule())));
        }

        return new PortForwardingReport(true, null, outcomes, plan.conflicts(), rejected);
    }

    private void apply(PlannedAction action) {
        switch (action.outcome()) {
            case CREATED -> redirectionRequestService.createRule(action.rule());
            case OPENED, CLOSED, REROUTED -> redirectionRequestService.updateRule(action.rule());
            case DELETED -> redirectionRequestService.deleteRule(action.rule());
            case UNCHANGED -> { }
        }
    }

    private String describe(PortRule rule) {
        return rule.describe() + " -> " + rule.lanIp() + ":" + rule.lanPort()
                + (rule.open() ? " (ouvert)" : " (fermé)");
    }

    private record PlannedAction(Outcome outcome, PortRule rule) {
    }

    private record Plan(List<PlannedAction> actions, List<String> conflicts) {
    }
}
