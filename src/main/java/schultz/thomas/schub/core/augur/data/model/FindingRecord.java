package schultz.thomas.schub.core.augur.data.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import schultz.thomas.schub.core.augur.business.engine.Evaluator;

import java.time.Instant;
import java.util.List;
import java.util.Map;

// L'évaluation d'un pattern sur un sujet, émise ou non : elle garde la version qui l'a produite et sa preuve.
// stale : une version plus récente est active ; l'ancienne reste affichée jusqu'à son recalcul.
@Document("augur_findings")
public record FindingRecord(
        @Id String id,
        @Indexed String subject,
        PatternVersion.Scope scope,
        String puuid,
        String matchId,
        @Indexed String patternKey,
        int version,
        double degree,
        boolean emitted,
        boolean excepted,
        List<Evaluator.ConditionTrace> conditions,
        Map<String, String> context,
        Instant computedAt,
        boolean stale
) {

    public static String idOf(String subject, String patternKey) {
        return subject + "|" + patternKey;
    }

    public static String gameSubject(String matchId, String puuid) {
        return "game:" + matchId + ":" + puuid;
    }

    public static String habitSubject(String puuid, Integer days) {
        return "habit:" + puuid + ":" + (days == null ? "all" : days);
    }
}
