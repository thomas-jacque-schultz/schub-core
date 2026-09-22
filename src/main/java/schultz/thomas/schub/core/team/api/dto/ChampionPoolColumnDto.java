package schultz.thomas.schub.core.team.api.dto;

import schultz.thomas.schub.core.team.business.model.GameRole;

import java.util.List;

/**
 * Une colonne du panneau : un poste, les champions qu'on s'y autorise, et qui peut les prendre.
 *
 * @param unavailableMembers ceux qui tiennent ce poste et dont on ne sait pas les maîtrises —
 *                           compte Riot non lié, connecteur muet, aucune maîtrise. Ils sont
 *                           rendus ici, une fois pour la colonne, plutôt que répétés sous chaque
 *                           champion : ce qui est à dire d'eux ne dépend pas du champion
 */
public record ChampionPoolColumnDto(
        GameRole role,
        List<ChampionPoolEntryDto> champions,
        List<ChampionPoolMemberDto> unavailableMembers,
        int hiddenByFloor
) {
}
