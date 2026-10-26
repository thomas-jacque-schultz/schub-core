package schultz.thomas.schub.core.lol.stats.api.dto;

import jakarta.annotation.Nullable;
import java.util.List;

// Les duos de l'effectif (Schub#24) et la répartition des ressources par poste (Schub#25), sur les parties
// d'équipe de la période.
public record TeamSynergyDto(
        long games,
        int minimumDuoGames,
        List<Duo> duos,
        List<Resource> resources,
        int minimumClosenessGames,
        List<Closeness> closeness
) {

    // Schub#154 : part des images à moins de 2 000 unités, avant et après 15 min, et sur toute la partie.
    public record Closeness(String memberId, @Nullable String displayName, List<Mate> mates) {
    }

    public record Mate(String memberId, @Nullable String displayName, long games, @Nullable Double earlyShare,
                       @Nullable Double lateShare, @Nullable Double share) {
    }

    // expected : moyenne des taux de victoire de chacun sans l'autre, sur les mêmes parties d'équipe.
    public record Duo(String memberA, @Nullable String nameA, String memberB, @Nullable String nameB, long games,
                      long wins, @Nullable Double winRate, @Nullable Double expected, @Nullable Double delta) {
    }

    // Parts moyennes de l'or et des dégâts de l'équipe tenues par ce poste ; seuil = médiane de l'équipe.
    public record Resource(String position, long games, @Nullable Double goldShareInWins,
                           @Nullable Double goldShareInLosses, @Nullable Double damageShareInWins,
                           @Nullable Double damageShareInLosses, @Nullable Double goldShareThreshold, long gamesAbove,
                           @Nullable Double winRateAbove, long gamesBelow, @Nullable Double winRateBelow,
                           @Nullable Double conversion) {
    }
}
