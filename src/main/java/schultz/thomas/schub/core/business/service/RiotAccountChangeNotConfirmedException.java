package schultz.thomas.schub.core.business.service;

import lombok.Getter;
import schultz.thomas.schub.core.api.dto.RiotAccountChangeDto;

/**
 * Le compte Riot lié allait être remplacé par un <em>autre</em>, sans que ce soit dit.
 *
 * <p>Elle porte ce que le changement emporte, pour que le refus soit une information et non une
 * fin de non-recevoir : l'écran a besoin de ces faits pour poser la question, et il ne les
 * obtiendrait autrement que par une route de prévisualisation de plus.</p>
 *
 * <p>Elle étend {@link IllegalStateException} pour rester un 409 même si le gestionnaire dédié
 * disparaissait un jour : le statut ne dépend pas d'une ligne qu'on peut oublier de recopier.</p>
 */
@Getter
public class RiotAccountChangeNotConfirmedException extends IllegalStateException {

    private final transient RiotAccountChangeDto change;

    public RiotAccountChangeNotConfirmedException(RiotAccountChangeDto change) {
        super("Ce compte Schub est déjà lié à " + change.previousRiotId()
                + " : confirmez le changement pour le remplacer par " + change.riotId());
        this.change = change;
    }
}
