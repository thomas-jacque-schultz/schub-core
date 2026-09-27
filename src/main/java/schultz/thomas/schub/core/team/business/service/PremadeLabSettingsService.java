package schultz.thomas.schub.core.team.business.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import schultz.thomas.schub.core.business.model.Permission;
import schultz.thomas.schub.core.business.service.PermissionEvaluator;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.team.api.dto.PremadeLabSettingsDto;
import schultz.thomas.schub.core.team.data.model.PremadeLabSettings;
import schultz.thomas.schub.core.team.data.repository.PremadeLabSettingsRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class PremadeLabSettingsService {

    private final PremadeLabSettingsRepository repository;
    private final PermissionEvaluator permissionEvaluator;

    public PremadeLabSettings current() {
        return repository.findById(PremadeLabSettings.ID).orElseGet(PremadeLabSettings::new);
    }

    public PremadeLabSettingsDto view(User actor) {
        permissionEvaluator.require(actor, Permission.INGEST_VIEW, null);
        return dto(current());
    }

    public PremadeLabSettingsDto update(User actor, PremadeLabSettingsDto request) {
        permissionEvaluator.require(actor, Permission.INGEST_MANAGE, null);
        if (request.unknownPlayerBudget() < 0 || request.budgetWindowMinutes() < 1) {
            throw new IllegalArgumentException("Budget positif ou nul, fenêtre d'au moins une minute");
        }
        PremadeLabSettings settings = current();
        settings.setUnknownPlayerBudget(request.unknownPlayerBudget());
        settings.setBudgetWindowMinutes(request.budgetWindowMinutes());
        log.info("Budget de recherche PremadeLab : {} joueurs inconnus par {} min, par {}",
                request.unknownPlayerBudget(), request.budgetWindowMinutes(), actor.getId());
        return dto(repository.save(settings));
    }

    private static PremadeLabSettingsDto dto(PremadeLabSettings settings) {
        return new PremadeLabSettingsDto(settings.getUnknownPlayerBudget(), settings.getBudgetWindowMinutes());
    }
}
