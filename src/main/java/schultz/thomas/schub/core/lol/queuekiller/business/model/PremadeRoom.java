package schultz.thomas.schub.core.lol.queuekiller.business.model;

import java.util.List;

public record PremadeRoom(String game, String room, List<Player> players) {

    public record Player(String displayName, String avatarUrl) {
    }
}
