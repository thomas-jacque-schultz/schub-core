package schultz.thomas.schub.core.team.api.dto;

// Moyenne des joueurs classés seulement : un non-classé n'est pas un Fer IV.
public record AverageRankDto(double value, String tier, String division, int counted) {
}
