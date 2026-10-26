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
        List<Closeness> closeness,
        Roles roles
) {

    // Schub#156 : par poste, quel que soit le joueur qui le tient, sur les parties d'équipe à timeline dont nos cinq
    // postes sont connus. objectives : objectifs neutres de ces parties ; jungleFights : combats dans notre jungle
    // où notre jungler était.
    public record Roles(int games, int objectives, int jungleFights, List<RoleLine> roles, List<PairLine> pairs,
                        ObjectiveTakes takes, JungleFights fights, List<Holder> holders) {
    }

    // Seul : part des images sans coéquipier à portée. Présence : part des objectifs, et des combats du jungler dans
    // sa jungle, où le poste était là.
    public record RoleLine(String position, @Nullable Double earlyAlone, @Nullable Double lateAlone,
                           @Nullable Double objectivePresence, @Nullable Double jungleFightPresence) {
    }

    public record PairLine(String position, String matePosition, @Nullable Double earlyShare, @Nullable Double lateShare) {
    }

    // Objectifs pris selon le nombre des nôtres présents (trois ou plus, ou moins), et avec ou sans le support.
    public record ObjectiveTakes(int crowded, int takenCrowded, int sparse, int takenSparse, int withSupport,
                                 int takenWithSupport, int withoutSupport, int takenWithoutSupport) {
    }

    // Combats du jungler dans sa jungle : avec au moins un coéquipier, ou seul ; gagné = plus de kills que de morts.
    public record JungleFights(int helped, int wonHelped, int alone, int wonAlone) {
    }

    // Un membre à un poste, sur ses parties à ce poste.
    public record Holder(String position, String memberId, @Nullable String displayName, int games, @Nullable Double lateAlone,
                         @Nullable Double lateWithJungler, @Nullable Double objectivePresence) {
    }

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
