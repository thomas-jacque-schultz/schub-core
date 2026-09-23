package schultz.thomas.schub.core.team.api.dto;

// Ganks : null quand les postes de la partie sont inconnus.
public record At15Dto(int gold, int xp, int cs, int damageToChampions, int kills, int deaths, int assists,
                      Integer ganksSuffered, Integer ganksSucceeded) {
}
