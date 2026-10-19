package schultz.thomas.schub.core.team.business.service;

import schultz.thomas.schub.core.team.data.model.GameRole;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;

// Une partie vue depuis « nous » : le camp de notre premier joueur, ou de celui qu'on suit.
record VueDePartie(RiotStatsGateway.SharedMatch partie, int cote) {

    static final int JOUEURS_PAR_CAMP = 5;

    static VueDePartie de(RiotStatsGateway.SharedMatch partie) {
        return new VueDePartie(partie, partie.players().isEmpty() ? 0 : partie.players().getFirst().side());
    }

    // -1 si le joueur n'y est pas : personne n'est alors de son camp.
    static VueDePartie duJoueur(RiotStatsGateway.SharedMatch partie, String puuid) {
        return new VueDePartie(partie, partie.players().stream().filter(j -> puuid.equals(j.puuid())).findFirst()
                .map(RiotStatsGateway.SharedMatchPlayer::side).orElse(-1));
    }

    // Nos joueurs d'abord, puis les autres joueurs de notre camp.
    List<RiotStatsGateway.SharedMatchPlayer> notreCamp() {
        List<RiotStatsGateway.SharedMatchPlayer> camp = new ArrayList<>(partie.players());
        camp.addAll(allies());
        return camp;
    }

    List<RiotStatsGateway.SharedMatchPlayer> allies() {
        return camp(partie.others(), cote, true);
    }

    List<RiotStatsGateway.SharedMatchPlayer> adverses() {
        return camp(partie.others(), cote, false);
    }

    static <J extends RiotStatsGateway.Participant> List<J> camp(Collection<J> joueurs, int cote, boolean notre) {
        return joueurs.stream().filter(joueur -> (joueur.side() == cote) == notre).toList();
    }

    // Le même poste dans le camp d'en face. Hors Faille, pas de poste, donc pas de vis-à-vis.
    static <J extends RiotStatsGateway.Participant> Optional<J> visAVis(RiotStatsGateway.Participant joueur,
                                                                       Collection<J> parmi) {
        if (joueur == null || !GameRole.estUnPoste(joueur.position())) {
            return Optional.empty();
        }
        return parmi.stream()
                .filter(autre -> autre.side() != joueur.side() && joueur.position().equals(autre.position()))
                .findFirst();
    }

    static <J extends RiotStatsGateway.Participant> Optional<J> auPoste(Collection<J> joueurs, String poste) {
        return joueurs.stream().filter(joueur -> poste.equals(joueur.position())).findFirst();
    }

    static Integer ecartA15(RiotStatsGateway.InsightPlayer nous, RiotStatsGateway.InsightPlayer eux,
                            ToIntFunction<RiotStatsGateway.At15> valeur) {
        return nous == null || eux == null || nous.at15() == null || eux.at15() == null ? null
                : valeur.applyAsInt(nous.at15()) - valeur.applyAsInt(eux.at15());
    }

    // Les dix chiffres de la timeline, sinon pas d'écart.
    static Integer ecartDesCampsA15(RiotStatsGateway.Insight insight, int cote,
                                    ToIntFunction<RiotStatsGateway.At15> valeur) {
        return ecartDesCamps(insight, cote, RiotStatsGateway.InsightPlayer::at15, valeur);
    }

    static Integer ecartDesCampsEnFin(RiotStatsGateway.Insight insight, int cote,
                                      ToIntFunction<RiotStatsGateway.AtEnd> valeur) {
        return ecartDesCamps(insight, cote, RiotStatsGateway.InsightPlayer::atEnd, valeur);
    }

    private static <T> Integer ecartDesCamps(RiotStatsGateway.Insight insight, int cote,
                                             Function<RiotStatsGateway.InsightPlayer, T> releve,
                                             ToIntFunction<T> valeur) {
        if (insight == null || insight.participants() == null || insight.participants().isEmpty()
                || insight.participants().stream().map(releve).anyMatch(Objects::isNull)) {
            return null;
        }
        int ecart = 0;
        for (RiotStatsGateway.InsightPlayer joueur : insight.participants()) {
            int chiffre = valeur.applyAsInt(releve.apply(joueur));
            ecart += joueur.side() == cote ? chiffre : -chiffre;
        }
        return ecart;
    }

    // L'or gagné par chaque camp, connu même sans timeline ; seulement à cinq contre cinq.
    Integer ecartDOrFinal() {
        if (partie.players().isEmpty() || partie.others() == null) {
            return null;
        }
        Map<String, RiotStatsGateway.SharedMatchPlayer> parPuuid = new LinkedHashMap<>();
        Stream.concat(partie.players().stream(), partie.others().stream())
                .forEach(j -> parPuuid.putIfAbsent(j.puuid(), j));
        if (camp(parPuuid.values(), cote, true).size() != JOUEURS_PAR_CAMP
                || parPuuid.size() != 2 * JOUEURS_PAR_CAMP) {
            return null;
        }
        int ecart = 0;
        for (RiotStatsGateway.SharedMatchPlayer joueur : parPuuid.values()) {
            ecart += joueur.side() == cote ? joueur.goldEarned() : -joueur.goldEarned();
        }
        return ecart;
    }
}
