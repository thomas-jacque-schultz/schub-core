package schultz.thomas.schub.core.team.api.dto;

import schultz.thomas.schub.core.team.business.model.GameRole;

/**
 * Une ligne proposée.
 *
 * @param championId la clé Data Dragon ({@code Ahri}, {@code MonkeyKing}…), pas un nom affiché :
 *                   les noms sont traduits et changent, les clés non
 * @param memberId   facultatif — une composition se prépare avant que l'effectif soit complet
 */
public record CompositionSlotRequest(GameRole role, String championId, String memberId) {
}
