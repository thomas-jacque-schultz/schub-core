package schultz.thomas.schub.core.lol.draft.api.dto;

import jakarta.annotation.Nullable;
import java.util.List;

// Conseils de draft (Schub#104) : tirés des parties où les joueurs du combo jouent ensemble.
public record DraftAdviceDto(
        int games,
        int duoThreshold,
        int trioThreshold,
        // Tous les duos, du meilleur au pire (borne basse de Wilson) : l'éditeur de composition s'en sert aussi.
        List<Combo> duos,
        List<Combo> trios,
        List<Combo> compositions,
        List<Combo> avoid,
        List<Combo> toTry,
        List<Ban> bans,
        List<LaneThreat> laneThreats
) {

    // kind : JGL_TOP, JGL_MID, JGL_SUP, ADC_SUP, JGL_MID_SUP, ou COMPOSITION.
    public record Combo(
            String kind,
            List<Pick> picks,
            long games,
            long wins,
            double winRate,
            double lowerBound,
            boolean lowConfidence,
            // Le même combo de champions, tous joueurs confondus ; absent si l'échantillon n'en a pas.
            @Nullable Long referenceGames,
            @Nullable Double referenceWinRate,
            // Ce que chaque joueur fait sur son champion sans l'autre, en moyenne (comme Schub#24).
            @Nullable Double expected
    ) {
    }

    public record Pick(@Nullable String memberId, @Nullable String memberName, String role, int championId,
                       @Nullable String championKey, @Nullable String championName, @Nullable String iconUrl) {
    }

    public record Ban(int championId, @Nullable String championKey, @Nullable String championName,
                      @Nullable String iconUrl, long gamesAgainst, long winsAgainst, double presence,
                      double winRateAgainst, double impact, boolean lowConfidence) {
    }

    // Écart d'or à 15 min d'un de nos joueurs face à ce champion, à son poste.
    public record LaneThreat(String memberId, @Nullable String memberName, String role, int championId,
                             @Nullable String championKey, @Nullable String championName, @Nullable String iconUrl,
                             long games, double goldDiffAt15) {
    }
}
