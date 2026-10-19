package schultz.thomas.schub.core.lol.stats.business.model;

import java.util.List;

// « Joue souvent avec » (Schub#96) : sur le mois en cours, les coéquipiers d'au moins 5 parties.
public record Companions(String month, int minimumGames, List<Companion> companions, List<String> suggestedTeam) {

    // withoutWinRate absent tant que l'échantillon sans lui est trop court pour conclure.
    public record Companion(String riotId, String slug, long games, long wins, double winRate, long withoutGames,
                            Double withoutWinRate) {
    }
}
