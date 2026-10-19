package schultz.thomas.schub.core.team.business.model;

import schultz.thomas.schub.core.team.data.model.GameRole;

import java.util.Arrays;
import java.util.List;

public enum ComboKind {

    JGL_TOP(GameRole.JGL, GameRole.TOP),
    JGL_MID(GameRole.JGL, GameRole.MID),
    JGL_SUP(GameRole.JGL, GameRole.SUP),
    ADC_SUP(GameRole.ADC, GameRole.SUP),
    JGL_MID_SUP(GameRole.JGL, GameRole.MID, GameRole.SUP);

    private final List<String> positions;

    ComboKind(GameRole... roles) {
        this.positions = Arrays.stream(roles).map(GameRole::position).toList();
    }

    public List<String> positions() {
        return positions;
    }
}
