package schultz.thomas.schub.core.data;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

// @Indexed et @CompoundIndex sont inertes (auto-index-creation à false) : chaque index se pose en ChangeUnit.
// Ce test relie chaque annotation à sa migration.
class IndexAnnotationsTest {

    private static final Set<String> COUVERTES = Set.of(
            "GameServer.java",          // V001 — slug unique
            "User.java",                // V002 — discordId, V007 — riotPuuid
            "Role.java",                // V002 — nom unique
            "StaticPortRuleEntity.java",// V011 — proto + port WAN unique
            "GameReview.java",          // V009 — revue unique, et l'index de lecture
            "Composition.java");        // V009 — index de lecture par équipe

    @Test
    @DisplayName("une annotation d'index sans migration qui la pose fait échouer ce test")
    void touteAnnotationDIndexEstCouverteParUneMigration() throws IOException {
        Path racine = Path.of("src/main/java/schultz/thomas/schub/core");

        try (Stream<Path> fichiers = Files.walk(racine)) {
            Set<String> annotes = new TreeSet<>();
            for (Path fichier : fichiers.filter(f -> f.toString().endsWith(".java")).toList()) {
                if (fichier.toString().contains("/migration/")) {
                    continue;
                }
                String source = Files.readString(fichier);
                if (source.contains("@Indexed") || source.contains("@CompoundIndex")) {
                    annotes.add(fichier.getFileName().toString());
                }
            }

            assertThat(annotes)
                    .as("""
                            Une annotation d'index ne crée rien ici : auto-index-creation est à false.
                            Ajoute le ChangeUnit qui pose l'index, puis inscris le fichier dans COUVERTES.""")
                    .containsExactlyInAnyOrderElementsOf(new TreeSet<>(List.copyOf(COUVERTES)));
        }
    }
}
