package schultz.thomas.schub.core.lol.stats.business.model;

// Moyenne des joueurs classés seulement : un non-classé n'est pas un Fer IV.
public record AverageRank(double value, String tier, String division, int counted) {
}
