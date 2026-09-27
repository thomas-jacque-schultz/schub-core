package schultz.thomas.schub.core.team.business.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import schultz.thomas.schub.core.team.data.model.PremadeLabSettings;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Fenêtre glissante par visiteur (adresse IP sans compte, compte sinon), en mémoire : un redémarrage la remet à zéro.
@Component
@RequiredArgsConstructor
public class VisitorBudget {

    private final PremadeLabSettingsService settings;
    private final Map<String, Deque<Instant>> demandes = new ConcurrentHashMap<>();

    // Vrai : dans le budget, la demande est comptée. Faux : au-delà, voie lente.
    public boolean consume(String visitor, Instant now) {
        PremadeLabSettings reglage = settings.current();
        Instant debut = now.minus(Duration.ofMinutes(reglage.getBudgetWindowMinutes()));
        Deque<Instant> fenetre = demandes.computeIfAbsent(visitor == null ? "anonyme" : visitor,
                cle -> new ArrayDeque<>());
        synchronized (fenetre) {
            while (!fenetre.isEmpty() && fenetre.peekFirst().isBefore(debut)) {
                fenetre.pollFirst();
            }
            if (fenetre.size() >= reglage.getUnknownPlayerBudget()) {
                return false;
            }
            fenetre.addLast(now);
            return true;
        }
    }
}
