package schultz.thomas.schub.core.team.api.dto;

import java.time.Instant;

/**
 * Une ligne de statistiques : un groupe de parties, réduit.
 *
 * <p>Tous les ratios sont nuls quand ils n'ont pas de dénominateur — jamais zéro, qui se lirait
 * comme un résultat.</p>
 *
 * @param key        la clé du groupe telle qu'elle vient : id de champion, poste, mode de jeu,
 *                   patch, {@code yyyy-MM}. Vide pour le total. Le mode de jeu est un nom de
 *                   file, pas un {@code queueId} — plusieurs identifiants désignent le même mode.
 * @param versusRest ce groupe comparé au reste des parties du même joueur.
 */
public record StatLineDto(
        String key,
        String label,
        String iconUrl,
        long games,
        long wins,
        Double winRate,
        Double kda,
        Double killsPerGame,
        Double deathsPerGame,
        Double assistsPerGame,
        Double csPerMinute,
        Double goldPerMinute,
        Double damagePerMinute,
        Double visionPerMinute,
        long afkGames,
        long secondsPlayed,
        Instant firstPlayedAt,
        Instant lastPlayedAt,
        StatComparisonDto versusRest
) {
}
