package schultz.thomas.schub.core.lol.stats.business.service;

import schultz.thomas.schub.core.lol.riot.business.service.RiotStatsGateway;
import schultz.thomas.schub.core.lol.riot.business.service.VueDePartie;
import schultz.thomas.schub.core.lol.stats.business.model.TeamSynergy;
import schultz.thomas.schub.core.lol.team.data.model.TeamMember;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Schub#156 : qui accompagne qui, par poste et non par joueur. Une partie compte si sa timeline est dérivée et si
// nos cinq postes y sont connus, un joueur chacun.
final class Soutiens {

    static final List<String> POSTES = List.of("TOP", "JUNGLE", "MIDDLE", "BOTTOM", "UTILITY");
    static final int GROUPE = 3;

    private static final String JUNGLE = "JUNGLE";
    private static final String SUPPORT = "UTILITY";

    private Soutiens() {
    }

    // Sommes d'images : seul avant / images avant / seul après / images après ; objectifs et combats où il était.
    private static final class Poste {
        final int[] seul = new int[4];
        int objectifs;
        int combats;
    }

    private static final class Titulaire {
        final String poste;
        final String memberId;
        int parties;
        final int[] seulApres = new int[2];
        final int[] avecJungler = new int[2];
        int objectifs;
        int presences;

        Titulaire(String poste, String memberId) {
            this.poste = poste;
            this.memberId = memberId;
        }
    }

    static TeamSynergy.Roles equipe(List<RiotStatsGateway.SharedMatch> parties,
                                    Map<String, RiotStatsGateway.Insight> insights,
                                    Map<String, TeamMember> parPuuid, Map<String, String> noms) {
        Map<String, Poste> postes = new LinkedHashMap<>();
        POSTES.forEach(p -> postes.put(p, new Poste()));
        Map<String, int[]> paires = new LinkedHashMap<>();
        Map<String, Titulaire> titulaires = new LinkedHashMap<>();
        int[] prises = new int[8];
        int[] combats = new int[4];
        int comptees = 0;
        int objectifs = 0;
        int combatsDuJungler = 0;

        for (RiotStatsGateway.SharedMatch partie : parties) {
            RiotStatsGateway.Insight insight = insights.get(partie.matchId());
            int camp = VueDePartie.de(partie).cote();
            Map<String, String> parPoste = insight == null ? null : nosPostes(insight, camp);
            if (parPoste == null || insight.isolations() == null || insight.contests() == null) {
                continue;
            }
            comptees++;
            Map<String, String> posteDe = new HashMap<>();
            parPoste.forEach((poste, puuid) -> posteDe.put(puuid, poste));
            String jungler = parPoste.get(JUNGLE);

            for (RiotStatsGateway.Isolation isolement : insight.isolations()) {
                String poste = posteDe.get(isolement.puuid());
                if (poste != null) {
                    int[] s = postes.get(poste).seul;
                    s[0] += isolement.earlyAlone();
                    s[1] += isolement.earlyMinutes();
                    s[2] += isolement.lateAlone();
                    s[3] += isolement.lateMinutes();
                }
            }
            if (insight.proximities() != null) {
                for (RiotStatsGateway.Proximity p : insight.proximities()) {
                    String a = posteDe.get(p.puuid());
                    String b = posteDe.get(p.matePuuid());
                    if (a != null && b != null) {
                        int[] s = paires.computeIfAbsent(cle(a, b), k -> new int[4]);
                        s[0] += p.earlyTogether();
                        s[1] += p.earlyMinutes();
                        s[2] += p.lateTogether();
                        s[3] += p.lateMinutes();
                    }
                }
            }

            int objectifsDeLaPartie = 0;
            Map<String, Integer> presencesDeLaPartie = new HashMap<>();
            for (RiotStatsGateway.Contest contest : insight.contests()) {
                List<String> nous = contest.present().stream().filter(posteDe::containsKey).toList();
                if (!contest.combat()) {
                    objectifs++;
                    objectifsDeLaPartie++;
                    nous.forEach(puuid -> {
                        postes.get(posteDe.get(puuid)).objectifs++;
                        presencesDeLaPartie.merge(puuid, 1, Integer::sum);
                    });
                    boolean pris = contest.side() == camp;
                    int groupe = nous.size() >= GROUPE ? 0 : 2;
                    prises[groupe]++;
                    prises[groupe + 1] += pris ? 1 : 0;
                    int support = nous.contains(parPoste.get(SUPPORT)) ? 4 : 6;
                    prises[support]++;
                    prises[support + 1] += pris ? 1 : 0;
                } else if (contest.side() == camp && nous.contains(jungler)) {
                    combatsDuJungler++;
                    nous.forEach(puuid -> postes.get(posteDe.get(puuid)).combats++);
                    boolean gagne = contest.sideKills() > contest.sideDeaths();
                    int seul = nous.size() > 1 ? 0 : 2;
                    combats[seul]++;
                    combats[seul + 1] += gagne ? 1 : 0;
                }
            }

            for (Map.Entry<String, String> place : parPoste.entrySet()) {
                TeamMember membre = parPuuid.get(place.getValue());
                if (membre == null) {
                    continue;
                }
                String puuid = place.getValue();
                Titulaire t = titulaires.computeIfAbsent(place.getKey() + ":" + membre.getMemberId(),
                        k -> new Titulaire(place.getKey(), membre.getMemberId()));
                t.parties++;
                insight.isolations().stream().filter(i -> i.puuid().equals(puuid)).findFirst().ifPresent(i -> {
                    t.seulApres[0] += i.lateAlone();
                    t.seulApres[1] += i.lateMinutes();
                });
                if (!JUNGLE.equals(place.getKey())) {
                    Proximites.entre(insight, puuid, jungler).ifPresent(p -> {
                        t.avecJungler[0] += p.lateTogether();
                        t.avecJungler[1] += p.lateMinutes();
                    });
                }
                t.objectifs += objectifsDeLaPartie;
                t.presences += presencesDeLaPartie.getOrDefault(puuid, 0);
            }
        }

        int totalObjectifs = objectifs;
        int totalCombats = combatsDuJungler;
        List<TeamSynergy.RoleLine> lignes = new ArrayList<>();
        postes.forEach((poste, s) -> lignes.add(new TeamSynergy.RoleLine(poste,
                Proximites.part(s.seul[0], s.seul[1]), Proximites.part(s.seul[2], s.seul[3]),
                fraction(s.objectifs, totalObjectifs), JUNGLE.equals(poste) ? null : fraction(s.combats, totalCombats))));
        List<TeamSynergy.PairLine> lignesDePaires = new ArrayList<>();
        paires.forEach((cle, s) -> {
            String[] deux = cle.split(":");
            lignesDePaires.add(new TeamSynergy.PairLine(deux[0], deux[1], Proximites.part(s[0], s[1]),
                    Proximites.part(s[2], s[3])));
        });
        lignesDePaires.sort(Comparator.comparing((TeamSynergy.PairLine p) -> p.lateShare() == null ? -1 : p.lateShare())
                .reversed());
        List<TeamSynergy.Holder> parJoueur = titulaires.values().stream()
                .sorted(Comparator.comparingInt((Titulaire t) -> POSTES.indexOf(t.poste))
                        .thenComparing(Comparator.comparingInt((Titulaire t) -> t.parties).reversed()))
                .map(t -> new TeamSynergy.Holder(t.poste, t.memberId, noms.get(t.memberId), t.parties,
                        Proximites.part(t.seulApres[0], t.seulApres[1]),
                        JUNGLE.equals(t.poste) ? null : Proximites.part(t.avecJungler[0], t.avecJungler[1]),
                        fraction(t.presences, t.objectifs)))
                .toList();
        return new TeamSynergy.Roles(comptees, objectifs, combatsDuJungler, lignes, lignesDePaires,
                new TeamSynergy.ObjectiveTakes(prises[0], prises[1], prises[2], prises[3], prises[4], prises[5],
                        prises[6], prises[7]),
                new TeamSynergy.JungleFights(combats[0], combats[1], combats[2], combats[3]), parJoueur);
    }

    // null : un de nos postes manque, ou deux joueurs le revendiquent.
    static Map<String, String> nosPostes(RiotStatsGateway.Insight insight, int camp) {
        Map<String, String> parPoste = new HashMap<>();
        for (RiotStatsGateway.InsightPlayer joueur : insight.participants()) {
            if (joueur.side() == camp && joueur.position() != null
                    && (!POSTES.contains(joueur.position()) || parPoste.put(joueur.position(), joueur.puuid()) != null)) {
                return null;
            }
        }
        return parPoste.size() == POSTES.size() ? parPoste : null;
    }

    private static String cle(String a, String b) {
        return POSTES.indexOf(a) < POSTES.indexOf(b) ? a + ":" + b : b + ":" + a;
    }

    private static Double fraction(int part, int total) {
        return total == 0 ? null : (double) part / total;
    }
}
