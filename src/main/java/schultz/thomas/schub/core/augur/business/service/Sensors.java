package schultz.thomas.schub.core.augur.business.service;

import schultz.thomas.schub.core.augur.business.engine.Signals;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

// Les capteurs : ce qui lit les données d'un sujet et les rend en signaux, valeur et centile dans le palier.
public interface Sensors {

    // Les joueurs d'une partie, par puuid.
    Map<String, Signals> game(String matchId);

    // Les moyennes d'un joueur sur la période, à son poste principal.
    Optional<Signals> habit(String puuid, Instant since);
}
