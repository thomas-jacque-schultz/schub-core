package schultz.thomas.schub.core.business.service;

import lombok.Getter;
import schultz.thomas.schub.core.business.model.RiotAccountChange;

@Getter
public class RiotAccountChangeNotConfirmedException extends IllegalStateException {

    private final transient RiotAccountChange change;

    public RiotAccountChangeNotConfirmedException(RiotAccountChange change) {
        super("Ce compte Schub est déjà lié à " + change.previousRiotId()
                + " : confirmez le changement pour le remplacer par " + change.riotId());
        this.change = change;
    }
}
