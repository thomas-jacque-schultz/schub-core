package schultz.thomas.schub.core.team.api.dto;

import java.util.List;

// La page publique d'un joueur recherché : son profil tout de suite, ses statistiques dès que ses parties arrivent.
public record SearchedPlayerDto(
        String gameName,
        String tagLine,
        String slug,
        boolean known,
        long knownGames,
        // Son aperçu est en cours de collecte : ses dernières parties arrivent.
        boolean collecting,
        List<RankedStandingDto> rankings,
        List<MasteryDto> masteries,
        MyStatsDto stats
) {

    public record MasteryDto(int championId, String championName, String iconUrl, int level, int points) {
    }
}
