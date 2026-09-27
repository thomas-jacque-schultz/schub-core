package schultz.thomas.schub.core.augur.api.dto;

import java.util.List;
import java.util.Map;

// Ce qu'une version changerait, sur un échantillon de sujets déjà évalués : constats qui apparaissent ou disparaissent.
public record ImpactDto(int sampled, int appearing, int disappearing, int unchanged,
                        Map<String, int[]> byTier, List<Example> examples) {

    // byTier : [apparaissent, disparaissent] par palier.
    public record Example(String subject, String tier, double currentDegree, double draftDegree) {
    }
}
