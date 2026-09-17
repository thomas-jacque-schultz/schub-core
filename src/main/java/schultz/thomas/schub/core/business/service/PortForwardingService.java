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

/**
 * Maintient les redirections du routeur alignées sur l'état voulu, en trois temps :
 * {@link PortRuleResolver} calcule ce qui devrait être ouvert, {@link #plan} compare au réel,
 * {@link #execute} applique. Seule la dernière étape touche le routeur.
 *
 * <p>Les règles que nous n'avons pas posées ne sont jamais modifiées : une redirection créée à
 * la main sur un port que l'on voudrait piloter est signalée comme conflit et laissée intacte.</p>
 *
 * <p>Les règles pilotées sont créées puis ouvertes/fermées plutôt que créées/supprimées : une
 * règle fermée est sans effet côté Internet tout en restant lisible sur le routeur, ce qui rend
 * l'inventaire des ports vérifiable d'un coup d'œil. {@code port-forwarding.prune-orphans}
 * bascule vers la suppression pour les règles qui ne sont plus déclarées nulle part.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortForwardingService {

    private final PortForwardingProperties properties;

    private final RedirectionRequestService redirectionRequestService;

    private final PortRuleResolver resolver;

    /** Sérialise les réconciliations : commandes Discord, front et scheduler peuvent les déclencher en parallèle. */
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * Recalcule l'ensemble des redirections voulues et applique l'écart.
     * Ne lève jamais : un routeur en panne ne doit pas empêcher un serveur de jeu de démarrer.
     */
    public PortForwardingReport reconcile() {
        return reconcile(null, false);
    }

    /**
     * Comme {@link #reconcile()}, en forçant l'état ouvert/fermé du serveur {@code overrideIdentifier}
     * au lieu de le déduire de son statut.
     */
    public PortForwardingReport reconcile(String overrideIdentifier, boolean overrideOpen) {
        String inactiveReason = inactiveReason();
        if (inactiveReason != null) {
            log.debug("Réconciliation des ports ignorée: {}", inactiveReason);
            return PortForwardingReport.skipped(inactiveReason);
        }

        lock.lock();
        try {
            return doReconcile(overrideIdentifier, overrideOpen);
        } catch (Exception e) {
            log.error("Réconciliation des redirections en échec", e);
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

    // --- décision (aucune I/O) ---------------------------------------------

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

    /**
     * Sort d'une règle que nous avons posée mais que plus rien ne réclame : serveur supprimé,
     * port retiré de sa fiche, règle permanente effacée du fichier. Null s'il n'y a rien à faire.
     */
    private PlannedAction decideOrphan(PortRule orphan) {
        if (properties.isPruneOrphans()) {
            return new PlannedAction(Outcome.DELETED, orphan);
        }
        return orphan.open() ? new PlannedAction(Outcome.CLOSED, orphan.withOpen(false)) : null;
    }

    // --- application --------------------------------------------------------

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
            case UNCHANGED -> { /* rien à écrire */ }
        }
    }

    private String describe(PortRule rule) {
        return rule.describe() + " -> " + rule.lanIp() + ":" + rule.lanPort()
                + (rule.open() ? " (ouvert)" : " (fermé)");
    }

    /** Ce qu'il faut faire d'une règle, décidé avant tout appel au routeur. */
    private record PlannedAction(Outcome outcome, PortRule rule) {
    }

    /** Le plan complet : les actions à appliquer, et les ports que l'on ne peut pas réclamer. */
    private record Plan(List<PlannedAction> actions, List<String> conflicts) {
    }
}
