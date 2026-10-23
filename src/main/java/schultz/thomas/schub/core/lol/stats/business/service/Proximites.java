package schultz.thomas.schub.core.lol.stats.business.service;

import schultz.thomas.schub.core.lol.riot.business.service.RiotStatsGateway;
import schultz.thomas.schub.core.lol.stats.business.model.EarlyGameView;
import schultz.thomas.schub.core.lol.team.data.model.GameRole;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

// Les distances entre coéquipiers d'une partie (Schub#152) : le support avec son ADC et son jungler, et le poste
// dont le jungler a été le plus proche.
final class Proximites {

    // Sous ce nombre de minutes comptées, une part ne dit rien.
    static final int MINUTES_MINIMUM = 5;

    private Proximites() {
    }

    static EarlyGameView.SupportProximity support(List<RiotStatsGateway.Proximity> paires,
                                                  List<RiotStatsGateway.SharedMatchPlayer> joueurs, int notreCote,
                                                  boolean notre) {
        Optional<String> support = auPoste(joueurs, notreCote, notre, GameRole.SUP);
        if (paires == null || support.isEmpty()) {
            return null;
        }
        Optional<RiotStatsGateway.Proximity> adc = auPoste(joueurs, notreCote, notre, GameRole.ADC)
                .flatMap(autre -> paire(paires, support.get(), autre));
        Optional<RiotStatsGateway.Proximity> jungler = auPoste(joueurs, notreCote, notre, GameRole.JGL)
                .flatMap(autre -> paire(paires, support.get(), autre));
        if (adc.isEmpty() && jungler.isEmpty()) {
            return null;
        }
        return new EarlyGameView.SupportProximity(
                adc.map(p -> part(p.earlyTogether(), p.early())).orElse(null),
                adc.map(RiotStatsGateway.Proximity::early).orElse(0),
                adc.map(RiotStatsGateway.Proximity::departures).orElse(0),
                jungler.map(p -> part(p.lateTogether(), p.late())).orElse(null),
                jungler.map(RiotStatsGateway.Proximity::late).orElse(0));
    }

    static EarlyGameView.JunglerProximity jungler(List<RiotStatsGateway.Proximity> paires,
                                                  List<RiotStatsGateway.SharedMatchPlayer> joueurs, int notreCote,
                                                  boolean notre) {
        Optional<String> jungler = auPoste(joueurs, notreCote, notre, GameRole.JGL);
        if (paires == null || jungler.isEmpty()) {
            return null;
        }
        return joueurs.stream()
                .filter(joueur -> (joueur.side() == notreCote) == notre && !joueur.puuid().equals(jungler.get()))
                .map(joueur -> paire(paires, jungler.get(), joueur.puuid())
                        .filter(p -> p.early() + p.late() >= MINUTES_MINIMUM)
                        .map(p -> new EarlyGameView.JunglerProximity(joueur.position(),
                                part(p.earlyTogether() + p.lateTogether(), p.early() + p.late()),
                                p.early() + p.late()))
                        .orElse(null))
                .filter(Objects::nonNull)
                .max(Comparator.comparingInt(EarlyGameView.JunglerProximity::share))
                .orElse(null);
    }

    static Integer part(int ensemble, int minutes) {
        return minutes < MINUTES_MINIMUM ? null : (int) Math.round(100.0 * ensemble / minutes);
    }

    private static Optional<String> auPoste(List<RiotStatsGateway.SharedMatchPlayer> joueurs, int notreCote,
                                            boolean notre, GameRole role) {
        return joueurs.stream()
                .filter(joueur -> (joueur.side() == notreCote) == notre
                        && GameRole.dePosition(joueur.position()).filter(role::equals).isPresent())
                .map(RiotStatsGateway.SharedMatchPlayer::puuid)
                .findFirst();
    }

    private static Optional<RiotStatsGateway.Proximity> paire(List<RiotStatsGateway.Proximity> paires, String a,
                                                              String b) {
        return paires.stream()
                .filter(p -> p.puuidA().equals(a) && p.puuidB().equals(b) || p.puuidA().equals(b) && p.puuidB().equals(a))
                .findFirst();
    }
}
