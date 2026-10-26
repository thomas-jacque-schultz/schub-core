package schultz.thomas.schub.core.lol.stats.business.service;

import schultz.thomas.schub.core.lol.riot.business.service.RiotStatsGateway;
import schultz.thomas.schub.core.lol.stats.business.model.EarlyGameView;
import schultz.thomas.schub.core.lol.stats.business.model.TeamSynergy;
import schultz.thomas.schub.core.lol.team.data.model.TeamMember;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Les distances entre coéquipiers que le connecteur tire de la timeline (Schub#151 à #154).
final class Proximites {

    // Même seuil que la projection du support dans le connecteur.
    static final int IMAGES_MINIMUM = 5;
    static final int PARTIES_MINIMUM = 5;

    private static final String SUPPORT = "UTILITY";
    private static final String ADC = "BOTTOM";
    private static final String JUNGLE = "JUNGLE";

    private Proximites() {
    }

    static EarlyGameView.SupportLine support(RiotStatsGateway.Insight insight, int camp,
                                             Map<String, TeamMember> parPuuid) {
        Optional<RiotStatsGateway.InsightPlayer> support = auPoste(insight, camp, SUPPORT);
        if (support.isEmpty()) {
            return null;
        }
        RiotStatsGateway.Proximity adc = auPoste(insight, camp, ADC)
                .flatMap(lui -> entre(insight, support.get().puuid(), lui.puuid())).orElse(null);
        RiotStatsGateway.Proximity jungler = auPoste(insight, camp, JUNGLE)
                .flatMap(lui -> entre(insight, support.get().puuid(), lui.puuid())).orElse(null);
        Double partAdc = adc == null ? null : part(adc.earlyTogether(), adc.earlyMinutes());
        Double partJungler = jungler == null ? null : part(jungler.lateTogether(), jungler.lateMinutes());
        if (partAdc == null && partJungler == null) {
            return null;
        }
        return new EarlyGameView.SupportLine(membre(support.get(), parPuuid), support.get().championId(), partAdc,
                partAdc == null ? null : adc.earlyDepartures(), partJungler);
    }

    // Sur toute la partie : avec qui le jungler a le plus joué.
    static EarlyGameView.JunglerLine jungler(RiotStatsGateway.Insight insight, int camp,
                                             Map<String, TeamMember> parPuuid) {
        Optional<RiotStatsGateway.InsightPlayer> jungler = auPoste(insight, camp, JUNGLE);
        if (jungler.isEmpty()) {
            return null;
        }
        EarlyGameView.JunglerLine proche = null;
        for (RiotStatsGateway.InsightPlayer autre : insight.participants()) {
            if (autre.side() != camp || autre.puuid().equals(jungler.get().puuid())
                    || autre.position() == null || JUNGLE.equals(autre.position())) {
                continue;
            }
            Double part = entre(insight, jungler.get().puuid(), autre.puuid())
                    .map(p -> part(p.earlyTogether() + p.lateTogether(), p.earlyMinutes() + p.lateMinutes()))
                    .orElse(null);
            if (part != null && (proche == null || part > proche.share())) {
                proche = new EarlyGameView.JunglerLine(membre(jungler.get(), parPuuid), jungler.get().championId(),
                        autre.position(), part);
            }
        }
        return proche;
    }

    // Parts groupées sur les parties : une partie longue pèse ses images, pas une voix.
    static List<TeamSynergy.Closeness> equipe(List<RiotStatsGateway.SharedMatch> parties,
                                              Map<String, RiotStatsGateway.Insight> insights,
                                              Map<String, TeamMember> parPuuid, Map<String, String> noms) {
        List<String> puuids = new ArrayList<>(parPuuid.keySet());
        List<TeamSynergy.Closeness> membres = new ArrayList<>();
        for (String moi : puuids) {
            List<TeamSynergy.Mate> proches = new ArrayList<>();
            for (String lui : puuids) {
                if (lui.equals(moi)) {
                    continue;
                }
                int[] somme = new int[5];
                for (RiotStatsGateway.SharedMatch partie : parties) {
                    RiotStatsGateway.Insight insight = insights.get(partie.matchId());
                    if (insight == null || partie.splitSides()) {
                        continue;
                    }
                    entre(insight, moi, lui).ifPresent(p -> {
                        somme[0]++;
                        somme[1] += p.earlyTogether();
                        somme[2] += p.earlyMinutes();
                        somme[3] += p.lateTogether();
                        somme[4] += p.lateMinutes();
                    });
                }
                if (somme[0] == 0) {
                    continue;
                }
                boolean assez = somme[0] >= PARTIES_MINIMUM;
                String id = parPuuid.get(lui).getMemberId();
                proches.add(new TeamSynergy.Mate(id, noms.get(id), somme[0],
                        assez ? part(somme[1], somme[2]) : null,
                        assez ? part(somme[3], somme[4]) : null,
                        assez ? part(somme[1] + somme[3], somme[2] + somme[4]) : null));
            }
            proches.sort(Comparator.comparing((TeamSynergy.Mate m) -> m.share() == null ? -1 : m.share()).reversed());
            String id = parPuuid.get(moi).getMemberId();
            membres.add(new TeamSynergy.Closeness(id, noms.get(id), proches));
        }
        return membres;
    }

    private static Optional<RiotStatsGateway.InsightPlayer> auPoste(RiotStatsGateway.Insight insight, int camp,
                                                                    String poste) {
        List<RiotStatsGateway.InsightPlayer> trouves = insight.participants().stream()
                .filter(joueur -> joueur.side() == camp && poste.equals(joueur.position()))
                .toList();
        return trouves.size() == 1 ? Optional.of(trouves.getFirst()) : Optional.empty();
    }

    private static Optional<RiotStatsGateway.Proximity> entre(RiotStatsGateway.Insight insight, String a, String b) {
        if (insight.proximities() == null) {
            return Optional.empty();
        }
        return insight.proximities().stream()
                .filter(p -> p.puuid().equals(a) && p.matePuuid().equals(b)
                        || p.puuid().equals(b) && p.matePuuid().equals(a))
                .findFirst();
    }

    private static String membre(RiotStatsGateway.InsightPlayer joueur, Map<String, TeamMember> parPuuid) {
        TeamMember membre = parPuuid.get(joueur.puuid());
        return membre == null ? null : membre.getMemberId();
    }

    static Double part(int ensemble, int images) {
        return images < IMAGES_MINIMUM ? null : (double) ensemble / images;
    }
}
