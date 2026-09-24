package schultz.thomas.schub.core.team.business.service;

import schultz.thomas.schub.core.team.api.dto.ReferenceGridDto;
import schultz.thomas.schub.core.team.api.dto.TeamLevelDto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;

// Mêmes chiffres que riot_team_side dans le connecteur, calculés ici sur les parties de l'équipe.
final class TeamLevels {

    static final List<String> CLES = List.of("goldDiffAt15", "xpDiffAt15", "killsDiffAt15", "dragons", "grubs",
            "heralds", "ganksDecisive", "ganksConceded");

    private TeamLevels() {
    }

    static TeamLevelDto of(List<RiotStatsGateway.SharedMatch> parties, Map<String, RiotStatsGateway.Insight> insights,
                           ReferenceGridDto grille) {
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
            int cote = TeamGamesStatsService.cote(partie);
            Double rang = TeamOppositionService.moyenne(insight, true, cote);
            if (rang != null) {
                rangs.add(rang);
            }
            String palier = rang == null ? null : Notes.groupe(Rangs.palier(rang));
            Map<String, Integer> valeurs = valeurs(insight, cote);
            valeurs.forEach((cle, valeur) -> {
                if (valeur != null) {
                    cumuls.get(cle).ajoute(valeur, cle, palier, grille);
                }
            });
        }
        String palierEquipe = rangs.isEmpty() ? null
                : Rangs.palier(rangs.stream().mapToDouble(Double::doubleValue).average().orElseThrow());
        return new TeamLevelDto(analysees, palierEquipe, grille == null ? List.of() : grille.patches(),
                CLES.stream().map(cle -> cumuls.get(cle).dto(cle, grille)).toList());
    }

    static Map<String, Integer> valeurs(RiotStatsGateway.Insight insight, int cote) {
        Map<String, Integer> valeurs = new LinkedHashMap<>();
        valeurs.put("goldDiffAt15", ecart(insight, cote, RiotStatsGateway.At15::gold));
        valeurs.put("xpDiffAt15", ecart(insight, cote, RiotStatsGateway.At15::xp));
        valeurs.put("killsDiffAt15", ecart(insight, cote, RiotStatsGateway.At15::kills));
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

    // Les dix chiffres à 15 min, sinon pas d'écart.
    private static Integer ecart(RiotStatsGateway.Insight insight, int cote,
                                 ToIntFunction<RiotStatsGateway.At15> valeur) {
        if (insight.participants().stream().anyMatch(joueur -> joueur.at15() == null)) {
            return null;
        }
        int nous = insight.participants().stream().filter(j -> j.side() == cote)
                .mapToInt(j -> valeur.applyAsInt(j.at15())).sum();
        int eux = insight.participants().stream().filter(j -> j.side() != cote)
                .mapToInt(j -> valeur.applyAsInt(j.at15())).sum();
        return nous - eux;
    }

    private static final class Cumul {
        private int parties;
        private double somme;
        private int notees;
        private double sommeDansPalier;

        void ajoute(int valeur, String cle, String palier, ReferenceGridDto grille) {
            parties++;
            somme += valeur;
            ReferenceGridDto.Metric metrique = grille == null ? null : grille.metrics().get(cle);
            if (metrique == null) {
                return;
            }
            ReferenceGridDto.Tier sien = palier == null ? null : metrique.tiers().get(palier);
            if (sien != null) {
                notees++;
                sommeDansPalier += Notes.sens(Notes.repartition(grille.percentiles(), sien.values(), valeur),
                        metrique.polarity());
            }
        }

        TeamLevelDto.Metric dto(String cle, ReferenceGridDto grille) {
            ReferenceGridDto.Metric metrique = grille == null ? null : grille.metrics().get(cle);
            Double moyenne = parties == 0 ? null : somme / parties;
            return new TeamLevelDto.Metric(cle, metrique == null ? null : metrique.polarity(), parties, moyenne,
                    notees == 0 ? null : sommeDansPalier / notees,
                    metrique == null ? null : Notes.niveau(moyenne, metrique.rankMedians()));
        }
    }
}
