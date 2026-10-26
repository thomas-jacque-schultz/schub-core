package schultz.thomas.schub.core.lol.stats.business.model;

import java.util.List;

// Les duos de l'effectif (Schub#24) et la répartition des ressources par poste (Schub#25), sur les parties
// d'équipe de la période.
public record TeamSynergy(
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
    public record RoleLine(String position, Double earlyAlone, Double lateAlone,
                           Double objectivePresence, Double jungleFightPresence) {
    }

    public record PairLine(String position, String matePosition, Double earlyShare, Double lateShare) {
    }

    // Objectifs pris selon le nombre des nôtres présents (trois ou plus, ou moins), et avec ou sans le support.
    public record ObjectiveTakes(int crowded, int takenCrowded, int sparse, int takenSparse, int withSupport,
                                 int takenWithSupport, int withoutSupport, int takenWithoutSupport) {
    }

    // Combats du jungler dans sa jungle : avec au moins un coéquipier, ou seul ; gagné = plus de kills que de morts.
    public record JungleFights(int helped, int wonHelped, int alone, int wonAlone) {
    }

    // Un membre à un poste, sur ses parties à ce poste.
    public record Holder(String position, String memberId, String displayName, int games, Double lateAlone,
                         Double lateWithJungler, Double objectivePresence) {
    }

    // Schub#154 : la part des images passées à moins de 2 000 unités de chaque coéquipier, sur leurs parties
    // d'équipe à timeline. Sous minimumClosenessGames parties, les parts sont absentes.
    public record Closeness(String memberId, String displayName, List<Mate> mates) {
    }

    public record Mate(String memberId, String displayName, long games, Double earlyShare, Double lateShare,
                       Double share) {
    }

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
