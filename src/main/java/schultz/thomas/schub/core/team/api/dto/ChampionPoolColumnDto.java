package schultz.thomas.schub.core.team.api.dto;

import schultz.thomas.schub.core.team.business.model.GameRole;

import java.util.List;

/**
 * Une colonne du panneau : un poste, et ceux qui le tiennent.
 *
 * <p>Une liste et non un membre unique : le plan §D.2 bis a écarté « une équipe = cinq joueurs »
 * (point 3). Un roster réel a des remplaçants, et deux personnes peuvent tenir le même poste —
 * une colonne qui n'en accepterait qu'une en perdrait une sans le dire.</p>
 */
public record ChampionPoolColumnDto(GameRole role, List<ChampionPoolMemberDto> members) {
}
