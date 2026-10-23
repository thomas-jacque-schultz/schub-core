package schultz.thomas.schub.core.lol.queuekiller.api.dto;

import jakarta.annotation.Nullable;
import schultz.thomas.schub.core.lol.queuekiller.business.model.PremadeRoom;

import java.util.List;

public record PremadeRoomDto(String game, String room, List<PremadeRoomPlayerDto> players) {

    public record PremadeRoomPlayerDto(String displayName, @Nullable String avatarUrl) {
    }

    public static PremadeRoomDto of(PremadeRoom salon) {
        return new PremadeRoomDto(salon.game(), salon.room(), salon.players().stream()
                .map(joueur -> new PremadeRoomPlayerDto(joueur.displayName(), joueur.avatarUrl()))
                .toList());
    }
}
