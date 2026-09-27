package schultz.thomas.schub.core.augur.business.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import schultz.thomas.schub.core.augur.api.dto.FindingDto;
import schultz.thomas.schub.core.augur.business.engine.Evaluator;
import schultz.thomas.schub.core.augur.business.engine.Signals;
import schultz.thomas.schub.core.augur.data.model.FindingRecord;
import schultz.thomas.schub.core.augur.data.model.PatternVersion;
import schultz.thomas.schub.core.augur.data.repository.FindingRecordRepository;
import schultz.thomas.schub.core.augur.data.repository.PatternVersionRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Les constats d'un sujet, calculés à la demande et gardés avec la version qui les a produits. Un constat d'une
 * version remplacée (stale) reste servi tant que son recalcul n'a pas abouti : jamais d'écran vide.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AugurService {

    // Les moyennes d'un joueur bougent à chaque partie ; une partie jouée, jamais.
    static final Duration FRAICHEUR_HABITUDE = Duration.ofHours(1);

    public enum Visibility { ALL, NEUTRAL }

    private final PatternVersionRepository patterns;
    private final FindingRecordRepository findings;
    private final Sensors sensors;

    public List<FindingDto> game(String matchId, String puuid, Visibility visibility) {
        List<PatternVersion> actifs = actifs(PatternVersion.Scope.GAME);
        String sujet = FindingRecord.gameSubject(matchId, puuid);
        List<FindingRecord> connus = findings.findBySubjectIn(List.of(sujet));
        if (aJour(connus, actifs, null)) {
            return vues(connus, actifs, visibility);
        }
        Map<String, Signals> joueurs = sensors.game(matchId);
        joueurs.forEach((joueur, signaux) -> enregistre(FindingRecord.gameSubject(matchId, joueur),
                PatternVersion.Scope.GAME, joueur, matchId, actifs, signaux));
        return vues(findings.findBySubjectIn(List.of(sujet)), actifs, visibility);
    }

    public List<FindingDto> habit(String puuid, Integer days, Visibility visibility) {
        List<PatternVersion> actifs = actifs(PatternVersion.Scope.HABIT);
        String sujet = FindingRecord.habitSubject(puuid, days);
        List<FindingRecord> connus = findings.findBySubjectIn(List.of(sujet));
        if (aJour(connus, actifs, FRAICHEUR_HABITUDE)) {
            return vues(connus, actifs, visibility);
        }
        Optional<Signals> signaux = sensors.habit(puuid, depuis(days));
        if (signaux.isEmpty()) {
            return vues(connus, actifs, visibility);
        }
        enregistre(sujet, PatternVersion.Scope.HABIT, puuid, null, actifs, signaux.get());
        return vues(findings.findBySubjectIn(List.of(sujet)), actifs, visibility);
    }

    public List<FindingDto> team(String teamId, Integer days) {
        List<PatternVersion> actifs = actifs(PatternVersion.Scope.TEAM);
        String sujet = FindingRecord.teamSubject(teamId, days);
        List<FindingRecord> connus = findings.findBySubjectIn(List.of(sujet));
        if (aJour(connus, actifs, FRAICHEUR_HABITUDE)) {
            return vues(connus, actifs, Visibility.ALL);
        }
        Optional<Signals> signaux = sensors.team(teamId, depuis(days));
        if (signaux.isEmpty()) {
            return vues(connus, actifs, Visibility.ALL);
        }
        enregistre(sujet, PatternVersion.Scope.TEAM, null, teamId, actifs, signaux.get());
        return vues(findings.findBySubjectIn(List.of(sujet)), actifs, Visibility.ALL);
    }

    // La trace complète, émis ou non : pour régler les seuils (section « Débogage des calculs »).
    public List<Evaluator.Evaluation> trace(PatternVersion.Scope scope, String puuid, String matchId, Integer days) {
        Optional<Signals> signaux = scope == PatternVersion.Scope.GAME
                ? Optional.ofNullable(sensors.game(matchId).get(puuid))
                : sensors.habit(puuid, depuis(days));
        return signaux.map(s -> List.copyOf(Evaluator.evaluateAll(actifs(scope), s).values())).orElseGet(List::of);
    }

    // Recalcule les constats d'une version remplacée, en un passage : un sujet qu'on ne sait plus lire reste
    // périmé, et donc affiché, plutôt que de faire boucler le recalcul.
    public int refreshStale(String patternKey, int limite) {
        List<FindingRecord> perimes = findings.findByPatternKeyAndStaleTrue(patternKey,
                org.springframework.data.domain.PageRequest.of(0, limite));
        java.util.Set<String> vus = new java.util.HashSet<>();
        int faits = 0;
        for (FindingRecord perime : perimes) {
            String cle = perime.scope() == PatternVersion.Scope.GAME ? perime.matchId() : perime.subject();
            if (!vus.add(cle)) {
                continue;
            }
            try {
                if (perime.scope() == PatternVersion.Scope.GAME) {
                    game(perime.matchId(), perime.puuid(), Visibility.ALL);
                } else if (perime.scope() == PatternVersion.Scope.TEAM) {
                    team(perime.matchId(), jours(perime.subject()));
                } else {
                    habit(perime.puuid(), jours(perime.subject()), Visibility.ALL);
                }
                faits++;
            } catch (RuntimeException e) {
                log.warn("Constat {} non recalculé : {}", perime.id(), e.getMessage());
            }
        }
        return faits;
    }

    List<PatternVersion> actifs(PatternVersion.Scope scope) {
        return patterns.findByStatus(PatternVersion.Status.ACTIVE).stream()
                .filter(p -> p.getScope() == scope)
                .sorted(Comparator.comparing(PatternVersion::getKey))
                .toList();
    }

    private static boolean aJour(List<FindingRecord> connus, List<PatternVersion> actifs, Duration fraicheur) {
        Map<String, FindingRecord> parCle = connus.stream()
                .collect(Collectors.toMap(FindingRecord::patternKey, Function.identity(), (a, b) -> a));
        Instant limite = fraicheur == null ? null : Instant.now().minus(fraicheur);
        return actifs.stream().allMatch(p -> {
            FindingRecord f = parCle.get(p.getKey());
            return f != null && f.version() == p.getVersion() && !f.stale()
                    && (limite == null || f.computedAt().isAfter(limite));
        });
    }

    private void enregistre(String sujet, PatternVersion.Scope scope, String puuid, String matchId,
                            List<PatternVersion> actifs, Signals signaux) {
        Instant maintenant = Instant.now();
        List<FindingRecord> lot = new ArrayList<>();
        Evaluator.evaluateAll(actifs, signaux).values().forEach(e -> lot.add(new FindingRecord(
                FindingRecord.idOf(sujet, e.patternKey()), sujet, scope, puuid, matchId, e.patternKey(),
                e.version(), e.degree(), e.emitted(), e.excepted(), e.conditions(), signaux.context(),
                maintenant, false)));
        findings.saveAll(lot);
    }

    private static List<FindingDto> vues(List<FindingRecord> records, List<PatternVersion> actifs,
                                         Visibility visibility) {
        Map<String, PatternVersion> parCle = actifs.stream()
                .collect(Collectors.toMap(PatternVersion::getKey, Function.identity()));
        return records.stream()
                .filter(FindingRecord::emitted)
                .filter(r -> parCle.containsKey(r.patternKey()))
                .filter(r -> visibility == Visibility.ALL
                        || parCle.get(r.patternKey()).getPolarity() == PatternVersion.Polarity.NEUTRAL)
                .sorted(Comparator.comparingDouble(FindingRecord::degree).reversed())
                .map(r -> FindingDto.of(r, parCle.get(r.patternKey())))
                .toList();
    }

    private static Instant depuis(Integer days) {
        return days == null ? null : Instant.now().minus(Duration.ofDays(days));
    }

    private static Integer jours(String sujet) {
        String fin = sujet.substring(sujet.lastIndexOf(':') + 1);
        return "all".equals(fin) ? null : Integer.valueOf(fin);
    }
}
