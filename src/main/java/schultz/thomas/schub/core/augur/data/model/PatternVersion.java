package schultz.thomas.schub.core.augur.data.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Une version d'un pattern. Immuable une fois sortie du brouillon : la modifier crée la version suivante.
@Data
@Document("augur_patterns")
@CompoundIndex(name = "key_version_unique", def = "{'key': 1, 'version': 1}", unique = true)
public class PatternVersion {

    public enum Status { DRAFT, ACTIVE, RETIRED }

    // GAME : une partie d'un joueur. HABIT : ses moyennes sur la période.
    public enum Scope { GAME, HABIT }

    public enum Polarity { STRENGTH, WEAKNESS, NEUTRAL }

    public enum Category { BEHAVIOUR, PERFORMANCE, KNOWLEDGE, BUILD }

    // Ce que le joueur fait, ou ce qui en découle (Schub#12).
    public enum Nature { ACTION, CONSEQUENCE }

    @Id
    private String id;

    private String key;
    private int version;
    private Status status;
    private Scope scope;
    private Polarity polarity;
    private Category category;
    private Nature nature;

    private List<Condition> required = new ArrayList<>();
    private List<Condition> optional = new ArrayList<>();
    private List<Condition> exceptions = new ArrayList<>();

    // Part du degré que les conditions optionnelles peuvent retirer.
    private double optionalInfluence = 0.3;
    private double threshold = 0.5;

    // Par langue. La phrase cite la preuve : {signal.value}, {signal.percentile}, {position}, {tier}.
    private Map<String, String> label = new LinkedHashMap<>();
    private Map<String, String> sentence = new LinkedHashMap<>();

    private String author;
    private String comment;
    private Instant createdAt;
    private Instant activatedAt;

    public static String idOf(String key, int version) {
        return key + "@" + version;
    }
}
