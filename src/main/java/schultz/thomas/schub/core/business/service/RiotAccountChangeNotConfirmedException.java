package schultz.thomas.schub.core.business.service;

import lombok.Getter;
import schultz.thomas.schub.core.api.dto.RiotAccountChangeDto;

@Getter
public class RiotAccountChangeNotConfirmedException extends IllegalStateException {

    private final transient RiotAccountChangeDto change;

    public RiotAccountChangeNotConfirmedException(RiotAccountChangeDto change) {
        super("Ce compte Schub est déjà lié à " + change.previousRiotId()
                + " : confirmez le changement pour le remplacer par " + change.riotId());
        this.change = change;
    }
}
