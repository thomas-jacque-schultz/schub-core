package schultz.thomas.schub.core.api.dto;

/**
 * Changer son nom d'affichage.
 *
 * @param displayName vide ou {@code null} <strong>rend le nom par défaut</strong>, celui de
 *                    Discord, au lieu d'échouer : c'est le seul moyen d'annuler une
 *                    personnalisation, et une route de suppression pour un champ facultatif
 *                    serait une route de plus pour la même intention
 */
public record DisplayNameRequest(String displayName) {
}
