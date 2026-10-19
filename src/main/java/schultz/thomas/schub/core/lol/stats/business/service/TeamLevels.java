package schultz.thomas.schub.core.lol.stats.business.service;

import schultz.thomas.schub.core.lol.riot.business.model.ReferenceGrid;
import schultz.thomas.schub.core.lol.riot.business.service.RiotStatsGateway;
import schultz.thomas.schub.core.lol.riot.business.service.VueDePartie;
import schultz.thomas.schub.core.lol.stats.business.model.TeamLevel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Mêmes chiffres que riot_team_side dans le connecteur, calculés ici sur les parties de l'équipe.
final class TeamLevels {

    static final List<String> CLES = List.of("goldDiffAt15", "xpDiffAt15", "killsDiffAt15", "dragons", "grubs",
            "heralds", "ganksDecisive", "ganksConceded");

    private TeamLevels() {
    }

    static TeamLevel of(List<RiotStatsGateway.SharedMatch> parties, Map<String, RiotStatsGateway.Insight> insights,
                           ReferenceGrid grille) {
        Map<String, Cumul> cumuls = new LinkedHashMap<>();
        CLES.forEach(cle -> cumuls.put(cle, new Cumul()));
        List<Double> rangs = new ArrayList<>();
        int analysees = 0;
        for (RiotStatsGateway.SharedMatch partie : parties) {
            RiotStatsGateway.Insight insight = insights.get(partie.matchId());
            if (insight == null || insight.early() == null) {
                continue;
            }
            analysees++;
            int cote = VueDePartie.de(partie).cote();
            Double rang = TeamOppositionService.moyenne(insight, true, cote);
            if (rang != null) {
                rangs.add(rang);
            }
            String palier = rang == null ? null : Notes.groupe(Rangs.palier(rang));
            Map<String, Integer> valeurs = valeurs(insight, cote);
            Map<String, Integer> fins = fins(insight, cote);
            valeurs.forEach((cle, valeur) -> {
                if (valeur != null) {
                    cumuls.get(cle).ajoute(valeur, cle, palier, grille);
                    Integer fin = fins.get(cle);
                    if (fin != null) {
                        cumuls.get(cle).ajouteFin(valeur, fin);
                    }
                }
            });
        }
        String palierEquipe = rangs.isEmpty() ? null
                : Rangs.palier(rangs.stream().mapToDouble(Double::doubleValue).average().orElseThrow());
        return new TeamLevel(analysees, palierEquipe, grille == null ? List.of() : grille.patches(),
                CLES.stream().map(cle -> cumuls.get(cle).dto(cle, grille)).toList());
    }

    static Map<String, Integer> valeurs(RiotStatsGateway.Insight insight, int cote) {
        Map<String, Integer> valeurs = new LinkedHashMap<>();
        valeurs.put("goldDiffAt15", VueDePartie.ecartDesCampsA15(insight, cote, RiotStatsGateway.At15::gold));
        valeurs.put("xpDiffAt15", VueDePartie.ecartDesCampsA15(insight, cote, RiotStatsGateway.At15::xp));
        valeurs.put("killsDiffAt15", VueDePartie.ecartDesCampsA15(insight, cote, RiotStatsGateway.At15::kills));
        RiotStatsGateway.Objectives objectifs = insight.early().objectives().stream()
                .filter(o -> o.side() == cote).findFirst().orElse(null);
        valeurs.put("dragons", objectifs == null ? 0 : objectifs.dragons());
        valeurs.put("grubs", objectifs == null ? 0 : objectifs.grubs());
        valeurs.put("heralds", objectifs == null ? 0 : objectifs.heralds());
        valeurs.put("ganksDecisive", (int) insight.early().ganks().stream()
                .filter(g -> g.attackerSide() == cote && g.decisive()).count());
        valeurs.put("ganksConceded", (int) insight.early().ganks().stream()
                .filter(g -> g.attackerSide() != cote && g.decisive()).count());
        return valeurs;
    }

    static Map<String, Integer> fins(RiotStatsGateway.Insight insight, int cote) {
        Map<String, Integer> fins = new LinkedHashMap<>();
        fins.put("goldDiffAt15", VueDePartie.ecartDesCampsEnFin(insight, cote, RiotStatsGateway.AtEnd::gold));
        fins.put("xpDiffAt15", VueDePartie.ecartDesCampsEnFin(insight, cote, RiotStatsGateway.AtEnd::xp));
        fins.put("killsDiffAt15", VueDePartie.ecartDesCampsEnFin(insight, cote, RiotStatsGateway.AtEnd::kills));
        RiotStatsGateway.EndObjectives objectifs = insight.endObjectives() == null ? null
                : insight.endObjectives().stream().filter(o -> o.side() == cote).findFirst().orElse(null);
        if (objectifs != null) {
            fins.put("dragons", objectifs.dragons());
            fins.put("heralds", objectifs.heralds());
        }
        return fins;
    }

    private static final class Cumul {
        private int parties;
        private double somme;
        private int notees;
        private double sommeDansPalier;
        private int fins;
        private double sommeFin;
        private double sommeChangement;

        void ajouteFin(int a15, int fin) {
            fins++;
            sommeFin += fin;
            sommeChangement += fin - a15;
        }

        void ajoute(int valeur, String cle, String palier, ReferenceGrid grille) {
            parties++;
            somme += valeur;
            ReferenceGrid.Metric metrique = grille == null ? null : grille.metrics().get(cle);
            if (metrique == null) {
                return;
            }
            ReferenceGrid.Tier sien = palier == null ? null : metrique.tiers().get(palier);
            if (sien != null) {
                notees++;
                sommeDansPalier += Notes.sens(Notes.repartition(grille.percentiles(), sien.values(), valeur),
                        metrique.polarity());
            }
        }

        TeamLevel.Metric dto(String cle, ReferenceGrid grille) {
            ReferenceGrid.Metric metrique = grille == null ? null : grille.metrics().get(cle);
            Double moyenne = parties == 0 ? null : somme / parties;
            return new TeamLevel.Metric(cle, metrique == null ? null : metrique.polarity(), parties, moyenne,
                    notees == 0 ? null : sommeDansPalier / notees,
                    metrique == null ? null : Notes.niveau(moyenne, metrique.rankMeans()),
                    fins, fins == 0 ? null : sommeFin / fins, fins == 0 ? null : sommeChangement / fins);
        }
    }
}
