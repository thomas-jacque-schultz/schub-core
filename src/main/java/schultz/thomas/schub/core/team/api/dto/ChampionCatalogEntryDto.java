package schultz.thomas.schub.core.team.api.dto;

/**
 * Un champion du catalogue, tel qu'on le donne à choisir.
 *
 * <p>Le catalogue entier accompagne le panneau au lieu d'avoir sa route : c'est ce qui garantit
 * que les icônes affichées et les champions retenus viennent du <em>même</em> patch. Deux appels
 * séparés, c'est deux versions possibles et une grille qui ment un jour sur deux.</p>
 */
public record ChampionCatalogEntryDto(int championId, String championKey, String name, String iconUrl) {
}
