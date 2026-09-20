package schultz.thomas.schub.core.team.api.dto;

import java.util.List;

/**
 * Enregistrer une composition.
 *
 * <p>{@code slots} en contient <strong>exactement cinq</strong>, un par poste ; toute autre
 * cardinalité est refusée en 400. C'est la seule contrainte de ce genre du domaine, et elle ne
 * remonte pas à l'équipe : une équipe n'est pas limitée à cinq joueurs, une composition en
 * désigne cinq.</p>
 *
 * @param patch la version Data Dragon au moment de la préparation, à figer côté appelant : le
 *              cœur ne parle pas à Data Dragon et n'a pas à deviner la version courante
 */
public record CompositionRequest(String name, List<CompositionSlotRequest> slots, String patch, String notes) {
}
