package schultz.thomas.schub.core.team.api.dto;

import java.util.List;

/**
 * Les champions retenus à un poste — <strong>la liste entière</strong>, qui remplace la
 * précédente.
 *
 * <p>Remplacer plutôt qu'ajouter/retirer un par un : le geste à l'écran est « voici ma sélection »,
 * et deux personnes qui cliquent en même temps se départagent alors sur la dernière écriture au
 * lieu de fusionner deux intentions en une troisième que personne n'a voulue.</p>
 */
public record PoolChampionsRequest(List<String> championKeys) {
}
