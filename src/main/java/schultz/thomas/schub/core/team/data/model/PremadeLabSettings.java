package schultz.thomas.schub.core.team.data.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

// Un seul document : les réglages de PremadeLab, édités depuis Schub.
@Data
@Document("premadelab_settings")
public class PremadeLabSettings {

    public static final String ID = "default";

    @Id
    private String id = ID;

    // Joueurs inconnus qu'un visiteur fait collecter vite, par fenêtre ; au-delà, voie lente.
    private int unknownPlayerBudget = 5;
    private int budgetWindowMinutes = 10;
}
