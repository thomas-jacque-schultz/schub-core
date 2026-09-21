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

/**
 * Dans ce dépôt, {@code @Indexed} et {@code @CompoundIndex} <strong>ne créent aucun index</strong> :
 * {@code auto-index-creation} vaut {@code false}, comme le veut le défaut de Spring Boot 3. Les
 * index se posent en ChangeUnit Mongock, où ils sont versionnés et relus.
 *
 * <p>Le piège a frappé quatre fois — V001, V002, V007, V009 — et une cinquième est passée sans être
 * vue : l'unicité protocole + port WAN des redirections n'a jamais existé jusqu'au V011.
 * L'annotation garde sa valeur de documentation ; ce test la relie à sa migration.</p>
 */
class IndexAnnotationsTest {

    /**
     * Chaque entrée est couverte par une migration. En ajouter une sans migration fait échouer ce
     * test — c'est son but : répondre « quel ChangeUnit la pose ? » avant de croire l'annotation.
     */
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
