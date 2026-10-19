package schultz.thomas.schub.core.lol.seo.business.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import schultz.thomas.schub.core.business.service.PermissionEvaluator;
import schultz.thomas.schub.core.data.model.Permission;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.lol.seo.data.model.PremadeLabSettings;
import schultz.thomas.schub.core.lol.seo.data.repository.PremadeLabSettingsRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class PremadeLabSettingsService {

    private final PremadeLabSettingsRepository repository;
    private final PermissionEvaluator permissionEvaluator;

    public PremadeLabSettings current() {
        return repository.findById(PremadeLabSettings.ID).orElseGet(PremadeLabSettings::new);
    }

    public PremadeLabSettings view(User actor) {
        permissionEvaluator.require(actor, Permission.INGEST_VIEW, null);
        return current();
    }

    public PremadeLabSettings update(User actor, int unknownPlayerBudget, int budgetWindowMinutes) {
        permissionEvaluator.require(actor, Permission.INGEST_MANAGE, null);
        if (unknownPlayerBudget < 0 || budgetWindowMinutes < 1) {
            throw new IllegalArgumentException("Budget positif ou nul, fenêtre d'au moins une minute");
        }
        PremadeLabSettings settings = current();
        settings.setUnknownPlayerBudget(unknownPlayerBudget);
        settings.setBudgetWindowMinutes(budgetWindowMinutes);
        log.info("Budget de recherche PremadeLab : {} joueurs inconnus par {} min, par {}",
                unknownPlayerBudget, budgetWindowMinutes, actor.getId());
        return repository.save(settings);
    }
}
