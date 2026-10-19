package schultz.thomas.schub.core.team.business.model;

import java.util.List;

// La page publique d'un joueur recherché : son profil tout de suite, ses statistiques dès que ses parties arrivent.
public record SearchedPlayer(
        String gameName,
        String tagLine,
        String slug,
        boolean known,
        long knownGames,
        // Son aperçu est en cours de collecte : ses dernières parties arrivent.
        boolean collecting,
        List<RankedStanding> rankings,
        List<Mastery> masteries,
        MyStats stats
) {

    public record Mastery(int championId, String championName, String iconUrl, int level, int points) {
    }
}
