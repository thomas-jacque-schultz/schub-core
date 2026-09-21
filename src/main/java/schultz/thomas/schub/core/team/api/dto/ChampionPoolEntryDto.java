package schultz.thomas.schub.core.team.api.dto;

import java.time.Instant;

/**
 * Un champion du pool d'un membre.
 *
 * <p>Les trois identités d'un champion cohabitent volontairement : {@code championId} est ce qui
 * recoupe les maîtrises et {@code match-v5}, {@code championKey} est la clé Data Dragon que
 * porte une composition ({@code CompositionSlotRequest}), et {@code name} est ce qu'on affiche —
 * traduit, et changeant. Les confondre, c'est enregistrer un nom traduit dans une composition et
 * ne plus le retrouver.</p>
 *
 * @param championKey {@code null}, comme {@code name} et {@code iconUrl}, si ce champion n'est pas
 *                    dans le catalogue de ce patch — un champion sorti après la version servie.
 *                    Il est rendu quand même : le taire ferait disparaître une maîtrise réelle
 * @param masteryLevel le niveau de maîtrise Riot. Aucun seuil n'est appliqué ici : « peut-il
 *                     jouer ce champion ? » est un jugement, et il appartient à celui qui regarde
 *                     la page
 */
public record ChampionPoolEntryDto(
        int championId,
        String championKey,
        String name,
        String iconUrl,
        int masteryLevel,
        int masteryPoints,
        Instant lastPlayedAt
) {
}
