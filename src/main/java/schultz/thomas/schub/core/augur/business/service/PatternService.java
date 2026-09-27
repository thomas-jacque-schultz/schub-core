package schultz.thomas.schub.core.augur.business.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import schultz.thomas.schub.core.augur.api.dto.ImpactDto;
import schultz.thomas.schub.core.augur.api.dto.PatternDto;
import schultz.thomas.schub.core.augur.api.dto.PatternRequest;
import schultz.thomas.schub.core.augur.business.engine.Evaluator;
import schultz.thomas.schub.core.augur.business.engine.Signals;
import schultz.thomas.schub.core.augur.data.model.FindingRecord;
import schultz.thomas.schub.core.augur.data.model.PatternVersion;
import schultz.thomas.schub.core.augur.data.repository.FindingRecordRepository;
import schultz.thomas.schub.core.augur.data.repository.PatternVersionRepository;
import schultz.thomas.schub.core.business.model.Permission;
import schultz.thomas.schub.core.business.service.PermissionEvaluator;
import schultz.thomas.schub.core.data.model.User;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Les règles vivent en base : une version active ne change jamais. Modifier, c'est écrire une nouvelle version en
 * brouillon, en mesurer l'impact sur un échantillon, puis l'activer ; les constats de l'ancienne version sont alors
 * recalculés en tâche de fond. Chaque version garde son auteur et son commentaire.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PatternService {

    private static final int ECHANTILLON = 60;
    private static final int RECALCUL_MAX = 10_000;

    private final PatternVersionRepository patterns;
    private final FindingRecordRepository findings;
    private final PermissionEvaluator permissionEvaluator;
    private final Sensors sensors;
    private final PatternRefresher refresher;

    public List<PatternDto> list(User actor) {
        permissionEvaluator.require(actor, Permission.AUGUR_PATTERN_EDIT, null);
        Map<String, PatternVersion> derniere = new LinkedHashMap<>();
        patterns.findAll().stream()
                .sorted(Comparator.comparing(PatternVersion::getKey).thenComparing(PatternVersion::getVersion))
                .forEach(p -> derniere.put(p.getKey(), p));
        return derniere.values().stream().map(PatternDto::of).toList();
    }

    public List<PatternDto> history(User actor, String key) {
        permissionEvaluator.require(actor, Permission.AUGUR_PATTERN_EDIT, null);
        return patterns.findByKeyOrderByVersionDesc(key).stream().map(PatternDto::of).toList();
    }

    // Un brouillon existant est remplacé ; sinon la version suivante naît en brouillon.
    public PatternDto draft(User actor, PatternRequest request) {
        permissionEvaluator.require(actor, Permission.AUGUR_PATTERN_EDIT, null);
        valide(request);
        List<PatternVersion> versions = patterns.findByKeyOrderByVersionDesc(request.key());
        PatternVersion brouillon = versions.stream()
                .filter(p -> p.getStatus() == PatternVersion.Status.DRAFT)
                .findFirst()
                .orElseGet(() -> {
                    PatternVersion p = new PatternVersion();
                    p.setKey(request.key());
                    p.setVersion(versions.isEmpty() ? 1 : versions.get(0).getVersion() + 1);
                    p.setId(PatternVersion.idOf(request.key(), p.getVersion()));
                    p.setStatus(PatternVersion.Status.DRAFT);
                    return p;
                });
        brouillon.setScope(request.scope());
        brouillon.setPolarity(request.polarity());
        brouillon.setCategory(request.category());
        brouillon.setNature(request.nature());
        brouillon.setRequired(liste(request.required()));
        brouillon.setOptional(liste(request.optional()));
        brouillon.setExceptions(liste(request.exceptions()));
        brouillon.setOptionalInfluence(request.optionalInfluence() == null ? 0.3 : request.optionalInfluence());
        brouillon.setThreshold(request.threshold() == null ? 0.5 : request.threshold());
        brouillon.setLabel(request.label() == null ? Map.of() : request.label());
        brouillon.setSentence(request.sentence() == null ? Map.of() : request.sentence());
        brouillon.setAuthor(actor.getId());
        brouillon.setComment(request.comment());
        brouillon.setCreatedAt(Instant.now());
        return PatternDto.of(patterns.save(brouillon));
    }

    // Recalcule, sur des sujets déjà évalués, ce que la version active et la version donnée concluent.
    public ImpactDto impact(User actor, String key, int version) {
        permissionEvaluator.require(actor, Permission.AUGUR_PATTERN_EDIT, null);
        PatternVersion candidat = version(key, version);
        Optional<PatternVersion> actif = patterns.findFirstByKeyAndStatus(key, PatternVersion.Status.ACTIVE);

        List<FindingRecord> sujets = findings.findByScopeOrderByComputedAtDesc(candidat.getScope(),
                PageRequest.of(0, ECHANTILLON * 20)).stream()
                .map(FindingRecord::subject).distinct().limit(ECHANTILLON)
                .map(sujet -> findings.findBySubjectIn(List.of(sujet)).get(0))
                .toList();

        int apparaissent = 0;
        int disparaissent = 0;
        int inchanges = 0;
        Map<String, int[]> parPalier = new LinkedHashMap<>();
        List<ImpactDto.Example> exemples = new ArrayList<>();
        Map<String, Map<String, Signals>> parties = new LinkedHashMap<>();
        for (FindingRecord sujet : sujets) {
            Optional<Signals> signaux = switch (candidat.getScope()) {
                case GAME -> Optional.ofNullable(parties.computeIfAbsent(sujet.matchId(), sensors::game).get(sujet.puuid()));
                case HABIT -> sensors.habit(sujet.puuid(), null);
                case TEAM -> sensors.team(sujet.matchId(), null);
            };
            if (signaux.isEmpty()) {
                continue;
            }
            List<PatternVersion> contexte = new ArrayList<>(patterns.findByStatus(PatternVersion.Status.ACTIVE)
                    .stream().filter(p -> p.getScope() == candidat.getScope()).toList());
            Evaluator.Evaluation avant = actif.map(p -> Evaluator.evaluateAll(contexte, signaux.get()).get(key))
                    .orElse(null);
            contexte.removeIf(p -> p.getKey().equals(key));
            contexte.add(candidat);
            Evaluator.Evaluation apres = Evaluator.evaluateAll(contexte, signaux.get()).get(key);

            boolean etait = avant != null && avant.emitted();
            String palier = signaux.get().context().getOrDefault("tier", "?");
            int[] compte = parPalier.computeIfAbsent(palier, p -> new int[2]);
            if (!etait && apres.emitted()) {
                apparaissent++;
                compte[0]++;
            } else if (etait && !apres.emitted()) {
                disparaissent++;
                compte[1]++;
            } else {
                inchanges++;
            }
            if (exemples.size() < 10 && etait != apres.emitted()) {
                exemples.add(new ImpactDto.Example(sujet.subject(), palier, avant == null ? 0 : avant.degree(),
                        apres.degree()));
            }
        }
        return new ImpactDto(sujets.size(), apparaissent, disparaissent, inchanges, parPalier, exemples);
    }

    public PatternDto activate(User actor, String key, int version, String comment) {
        permissionEvaluator.require(actor, Permission.AUGUR_PATTERN_EDIT, null);
        if (comment == null || comment.isBlank()) {
            throw new IllegalArgumentException("Un commentaire dit pourquoi cette version devient la règle");
        }
        PatternVersion cible = version(key, version);
        patterns.findFirstByKeyAndStatus(key, PatternVersion.Status.ACTIVE).ifPresent(ancienne -> {
            ancienne.setStatus(PatternVersion.Status.RETIRED);
            patterns.save(ancienne);
        });
        cible.setStatus(PatternVersion.Status.ACTIVE);
        cible.setActivatedAt(Instant.now());
        if (cible.getComment() == null || !cible.getComment().contains(comment)) {
            cible.setComment(cible.getComment() == null ? comment : cible.getComment() + " — " + comment);
        }
        PatternVersion active = patterns.save(cible);
        refresher.markStaleAndRefresh(key);
        log.info("Pattern {} : version {} active, par {} ({})", key, version, actor.getId(), comment);
        return PatternDto.of(active);
    }

    // Le retour arrière réactive la version précédente, avec la même invalidation.
    public PatternDto rollback(User actor, String key, String comment) {
        PatternVersion actif = patterns.findFirstByKeyAndStatus(key, PatternVersion.Status.ACTIVE)
                .orElseThrow(() -> new NoSuchElementException("Aucune version active du pattern " + key));
        PatternVersion precedente = patterns.findByKeyOrderByVersionDesc(key).stream()
                .filter(p -> p.getStatus() == PatternVersion.Status.RETIRED && p.getVersion() < actif.getVersion())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Aucune version précédente où revenir"));
        return activate(actor, key, precedente.getVersion(), comment);
    }

    private PatternVersion version(String key, int version) {
        return patterns.findByKeyAndVersion(key, version)
                .orElseThrow(() -> new NoSuchElementException("Aucune version " + version + " du pattern " + key));
    }

    private static void valide(PatternRequest request) {
        if (request.key() == null || !request.key().matches("[a-z0-9-]{3,60}")) {
            throw new IllegalArgumentException("Clé : 3 à 60 caractères, minuscules, chiffres et tirets");
        }
        if (request.scope() == null || request.polarity() == null || request.category() == null
                || request.nature() == null) {
            throw new IllegalArgumentException("Portée, polarité, catégorie et nature sont obligatoires");
        }
        if (request.required() == null || request.required().isEmpty()) {
            throw new IllegalArgumentException("Au moins une condition obligatoire");
        }
        if (request.comment() == null || request.comment().isBlank()) {
            throw new IllegalArgumentException("Un commentaire dit pourquoi la règle change");
        }
    }

    private static <T> List<T> liste(List<T> valeurs) {
        return valeurs == null ? new ArrayList<>() : new ArrayList<>(valeurs);
    }

    // Composant à part : @Async ne s'applique pas à un appel interne.
    @Service
    @RequiredArgsConstructor
    public static class PatternRefresher {

        private final org.springframework.data.mongodb.core.MongoTemplate mongo;
        private final AugurService augur;

        @Async
        public void markStaleAndRefresh(String key) {
            mongo.updateMulti(
                    org.springframework.data.mongodb.core.query.Query.query(
                            org.springframework.data.mongodb.core.query.Criteria.where("patternKey").is(key)),
                    new org.springframework.data.mongodb.core.query.Update().set("stale", true),
                    FindingRecord.class);
            int total = augur.refreshStale(key, RECALCUL_MAX);
            log.info("Pattern {} : {} sujets recalculés", key, total);
        }
    }
}
