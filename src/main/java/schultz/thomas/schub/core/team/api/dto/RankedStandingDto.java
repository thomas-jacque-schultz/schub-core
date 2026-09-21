package schultz.thomas.schub.core.team.api.dto;

import java.time.Instant;

/**
 * Le rang d'un joueur dans <strong>une</strong> file classée.
 *
 * <p>{@code league-v4} rend une entrée par file où le compte est classé — solo/duo, flex, et les
 * files de TFT si le compte y joue. Elles sont toutes servies : n'en garder qu'une ferait
 * disparaître un rang réel sans le dire.</p>
 *
 * @param queue         le mode de jeu nommé ({@code RANKED_SOLO}, {@code RANKED_FLEX},
 *                      {@code OTHER}), pour être traduit
 * @param riotQueueType le nom brut de Riot ({@code RANKED_SOLO_5x5}). Il est servi parce que
 *                      plusieurs files tombent sur {@code OTHER} : sans lui, deux lignes
 *                      distinctes s'afficheraient à l'identique
 * @param inactive      Riot marque ainsi un classement que l'inactivité menace de faire tomber
 */
public record RankedStandingDto(
        String queue,
        String riotQueueType,
        String tier,
        String division,
        int leaguePoints,
        int wins,
        int losses,
        boolean hotStreak,
        boolean inactive,
        Instant observedAt
) {
}
