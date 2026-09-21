package schultz.thomas.schub.core.team.api.dto;

/**
 * Un écart, jamais une note.
 *
 * <p>Il n'existe aucune référence mondiale accessible, et elle ne se fabrique pas : la seule
 * comparaison honnête est celle d'un joueur à lui-même ou à ses coéquipiers.</p>
 *
 * @param referenceGames parties sur lesquelles repose le terme de comparaison. Un écart calculé
 *                       contre quatre parties ne vaut pas un écart calculé contre quatre cents.
 */
public record StatComparisonDto(
        long referenceGames,
        Double winRateDelta,
        Double kdaDelta
) {
}
