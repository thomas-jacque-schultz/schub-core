package schultz.thomas.schub.core.team.api.dto;

import java.util.List;

// Les duos de l'effectif (Schub#24) et la répartition des ressources par poste (Schub#25), sur les parties
// d'équipe de la période.
public record TeamSynergyDto(
        long games,
        int minimumDuoGames,
        List<Duo> duos,
        List<Resource> resources
) {

    // expected : moyenne des taux de victoire de chacun sans l'autre, sur les mêmes parties d'équipe.
    public record Duo(String memberA, String nameA, String memberB, String nameB, long games, long wins,
                      Double winRate, Double expected, Double delta) {
    }

    // Parts moyennes de l'or et des dégâts de l'équipe tenues par ce poste ; seuil = médiane de l'équipe.
    public record Resource(String position, long games, Double goldShareInWins, Double goldShareInLosses,
                           Double damageShareInWins, Double damageShareInLosses, Double goldShareThreshold,
                           long gamesAbove, Double winRateAbove, long gamesBelow, Double winRateBelow,
                           Double conversion) {
    }
}
