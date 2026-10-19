package schultz.thomas.schub.core.team.business.model;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum GameRole {

    TOP("TOP", "Top"),
    JGL("JUNGLE", "Jungle"),
    MID("MIDDLE", "Mid"),
    ADC("BOTTOM", "ADC"),
    SUP("UTILITY", "Support");

    // Les postes au sens de Riot (teamPosition). Hors Faille, une partie n'en a pas : ni voie, ni vis-à-vis.
    public static final List<String> POSITIONS = Arrays.stream(values()).map(GameRole::position).toList();

    private final String position;
    private final String libelle;

    GameRole(String position, String libelle) {
        this.position = position;
        this.libelle = libelle;
    }

    public String position() {
        return position;
    }

    public String libelle() {
        return libelle;
    }

    public static Optional<GameRole> dePosition(String position) {
        return Arrays.stream(values()).filter(role -> role.position.equals(position)).findFirst();
    }

    public static boolean estUnPoste(String position) {
        return POSITIONS.contains(position);
    }
}
