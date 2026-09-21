package schultz.thomas.schub.core.team.data.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Une note de débrief : ce qu'un compte a écrit sur une place de l'effectif, pour une partie.
 *
 * <p>L'index unique porte le choix de fond : une revue par auteur, par sujet et par partie. Un
 * débrief n'est pas un fil de discussion, et sans cette contrainte le même joueur empile dix
 * notes sur la même partie sans que rien ne le signale.</p>
 *
 * <p>Aucune partie n'est stockée ici, seulement son {@code matchId} : les parties vivent une
 * seule fois, dans le connecteur (plan §D.2 ter).</p>
 */
@Data
@Document(collection = "team_game_reviews")
@CompoundIndex(name = "revue_unique",
        def = "{'matchId': 1, 'subjectMemberId': 1, 'authorUserId': 1}", unique = true)
public class GameReview {

    @Id
    private String id;

    @Indexed
    private String teamId;

    private String matchId;

    /** La place notée dans cette équipe — jamais un identifiant de compte. */
    private String subjectMemberId;

    /** L'id interne du compte qui a écrit. Sert à autoriser la retouche et à afficher l'auteur. */
    private String authorUserId;

    private String content;

    private Instant createdAt;
    private Instant updatedAt;
}
