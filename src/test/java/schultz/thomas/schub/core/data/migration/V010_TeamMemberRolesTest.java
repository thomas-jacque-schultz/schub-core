package schultz.thomas.schub.core.data.migration;

import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class V010_TeamMemberRolesTest {

    @Test
    @DisplayName("un poste unique devient une liste d'un poste")
    void convertitUnPosteEnListe() {
        List<Document> effectif = effectif(new Document("memberId", "m-1").append("role", "TOP"));

        assertThat(V010_TeamMemberRoles.convertit(effectif)).isTrue();
        assertThat(effectif.getFirst().get("roles")).isEqualTo(List.of("TOP"));
        assertThat(effectif.getFirst().containsKey("role")).isFalse();
    }

    @Test
    @DisplayName("un membre sans poste reçoit une liste vide, pas un null")
    void convertitUnMembreSansPoste() {
        List<Document> effectif = effectif(new Document("memberId", "m-coach").append("role", null));

        assertThat(V010_TeamMemberRoles.convertit(effectif)).isTrue();
        assertThat(effectif.getFirst().get("roles")).isEqualTo(List.of());
    }

    @Test
    @DisplayName("rejouée, elle ne déclare aucune modification et ne perd aucun poste")
    void rejouableSansEffet() {
        List<Document> effectif = effectif(
                new Document("memberId", "m-1").append("role", "TOP"),
                new Document("memberId", "m-2").append("role", "MID"));

        assertThat(V010_TeamMemberRoles.convertit(effectif)).isTrue();
        List<Object> apresLePremierPassage = effectif.stream().map(membre -> membre.get("roles")).toList();

        assertThat(V010_TeamMemberRoles.convertit(effectif)).isFalse();
        assertThat(effectif.stream().map(membre -> membre.get("roles")).toList())
                .isEqualTo(apresLePremierPassage);
    }

    @Test
    @DisplayName("un membre déjà en plusieurs postes n'est pas ramené à un seul")
    void neRamenePasUnMembreDejaConverti() {
        List<Document> effectif = effectif(new Document("memberId", "m-1")
                .append("roles", List.of("TOP", "MID"))
                .append("role", "TOP"));

        assertThat(V010_TeamMemberRoles.convertit(effectif)).isFalse();
        assertThat(effectif.getFirst().get("roles")).isEqualTo(List.of("TOP", "MID"));
        assertThat(effectif.getFirst().containsKey("role")).isFalse();
    }

    private static List<Document> effectif(Document... members) {
        return new ArrayList<>(List.of(members));
    }
}
