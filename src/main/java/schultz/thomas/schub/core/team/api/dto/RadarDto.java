package schultz.thomas.schub.core.team.api.dto;

import java.util.List;

// Les patchs sont ceux du jeu, pas ceux du joueur : deux joueurs comparés regardent la même période.
public record RadarDto(
        List<String> recentPatches,
        List<String> previousPatches,
        StatLineDto recent,
        StatLineDto previous
) {
}
