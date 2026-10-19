package schultz.thomas.schub.core.team.business.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.team.business.model.ComboKind;
import schultz.thomas.schub.core.team.business.model.DraftAdvice;
import schultz.thomas.schub.core.team.data.model.GameRole;
import schultz.thomas.schub.core.team.data.model.Team;
import schultz.thomas.schub.core.team.data.model.TeamChampionPool;
import schultz.thomas.schub.core.team.data.model.TeamMember;
import schultz.thomas.schub.core.team.data.repository.TeamChampionPoolRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

// Conseils de draft (Schub#104) : nos combos (#105), les bans (#106), les combos à essayer (#108).
// L'unité est (joueur, poste, champion) ; le repère général, tous joueurs confondus, n'est qu'un repère.
@Service
@RequiredArgsConstructor
public class DraftAdviceService {

    // Seuils de confiance du 05/10 : sous eux, le résultat reste affiché avec le logo.
    static final int DUO_SEUIL = 5;
    static final int TRIO_SEUIL = 4;
    static final int RETENUS = 3;
    static final int BANS = 5;
    static final int PIRES_MINIMUM = 2;
    static final int ATTENDU_MINIMUM = 3;
    static final int DEJA_ESSAYE = 3;
    // Un champion « joué » par un membre, hors pool : au moins 5 parties (réponse du 05/10 sur #108).
    static final int JOUE_MINIMUM = 5;
    static final int REPERE_SOLIDE = 30;
    static final int COUP_DE_COULOIR_MINIMUM = 2;
    static final double RETARD_SIGNIFICATIF = -300;
    static final double Z = 1.96;

    static final List<ComboKind> DUOS = List.of(ComboKind.JGL_TOP, ComboKind.JGL_MID, ComboKind.JGL_SUP,
            ComboKind.ADC_SUP);
    static final ComboKind TRIO = ComboKind.JGL_MID_SUP;

    private final TeamService teamService;
    private final RiotStatsGateway statsGateway;
    private final RiotChampionGateway championGateway;
    private final TeamGamesStatsService gamesStats;
    private final TeamChampionPoolRepository pools;
    private final RiotComboGateway combos;

    // Un choix dans un combo : un membre, à un poste, sur un champion.
    record Choix(String memberId, String position, int championId) {
    }

    static final class Compte {
        long parties;
        long victoires;

        void ajoute(boolean victoire) {
            parties++;
            victoires += victoire ? 1 : 0;
        }
    }

    public DraftAdvice of(User actor, String teamId, Integer days) {
        Team team = teamService.requireVisible(actor, teamId);
        List<TeamMember> joueurs = TeamPlayerStatsService.joueursDe(team);
        Map<String, TeamMember> parPuuid = TeamPlayerStatsService.parPuuid(joueurs);
        Map<String, String> noms = gamesStats.noms(joueurs);
        Map<Integer, RiotChampionGateway.Champion> catalogue = championGateway.catalogue()
                .map(RiotChampionGateway.Catalogue::parId).orElseGet(Map::of);
        if (parPuuid.size() < 2) {
            return new DraftAdvice(0, DUO_SEUIL, TRIO_SEUIL, List.of(), List.of(), List.of(), List.of(),
                    List.of(), List.of(), List.of());
        }
        List<RiotStatsGateway.SharedMatch> parties = statsGateway.sharedMatches(List.copyOf(parPuuid.keySet()), 2,
                        PlayerStatsService.depuis(days), TeamGamesStatsService.PARTIES_MAX)
                .map(RiotStatsGateway.SharedMatches::matches).orElseGet(List::of).stream()
                .filter(p -> p.win() != null && !p.splitSides() && !p.players().isEmpty())
                .toList();

        Map<List<Choix>, Compte> duos = new LinkedHashMap<>();
        Map<List<Choix>, Compte> trios = new LinkedHashMap<>();
        Map<List<Choix>, Compte> compos = new LinkedHashMap<>();
        Map<Choix, Map<String, Compte>> sansPartenaire = new HashMap<>();
        Map<Choix, Compte> seul = new HashMap<>();
        for (RiotStatsGateway.SharedMatch partie : parties) {
            boolean victoire = Boolean.TRUE.equals(partie.win());
            Map<String, Choix> parPoste = new LinkedHashMap<>();
            for (RiotStatsGateway.SharedMatchPlayer joueur : partie.players()) {
                TeamMember membre = parPuuid.get(joueur.puuid());
                if (membre != null && GameRole.estUnPoste(joueur.position())) {
                    parPoste.put(joueur.position(), new Choix(membre.getMemberId(), joueur.position(),
                            joueur.championId()));
                }
            }
            parPoste.values().forEach(choix -> seul.computeIfAbsent(choix, c -> new Compte()).ajoute(victoire));
            Set<String> presents = parPoste.values().stream().map(Choix::memberId).collect(Collectors.toSet());
            parPoste.values().forEach(choix -> joueurs.forEach(autre -> {
                if (!presents.contains(autre.getMemberId())) {
                    sansPartenaire.computeIfAbsent(choix, c -> new HashMap<>())
                            .computeIfAbsent(autre.getMemberId(), m -> new Compte()).ajoute(victoire);
                }
            }));
            for (ComboKind type : DUOS) {
                ajoute(duos, type, parPoste, victoire);
            }
            ajoute(trios, TRIO, parPoste, victoire);
            if (parPoste.size() >= 3) {
                List<Choix> compo = GameRole.POSITIONS.stream().map(parPoste::get).filter(Objects::nonNull).toList();
                compos.computeIfAbsent(compo, c -> new Compte()).ajoute(victoire);
            }
        }

        // Les appels au repère général prennent chacun quelques secondes : ils partent ensemble.
        CompletableFuture<List<DraftAdvice.Combo>> essais =
                Parallele.lance(() -> aEssayer(team, joueurs, parPuuid, noms, catalogue, duos));
        CompletableFuture<List<DraftAdvice.LaneThreat>> menaces =
                Parallele.lance(() -> menacesDeCouloir(parties, parPuuid, noms, catalogue));
        Rendu rendu = new Rendu(noms, catalogue, sansPartenaire);
        List<DraftAdvice.Combo> duosRendus = duos.entrySet().stream()
                .map(e -> rendu.combo(kindOf(e.getKey()), e.getKey(), e.getValue(), DUO_SEUIL))
                .sorted(Comparator.comparingDouble(DraftAdvice.Combo::lowerBound).reversed())
                .toList();
        List<DraftAdvice.Combo> triosRendus = trios.entrySet().stream()
                .map(e -> rendu.combo(TRIO.name(), e.getKey(), e.getValue(), TRIO_SEUIL))
                .sorted(Comparator.comparingDouble(DraftAdvice.Combo::lowerBound).reversed())
                .toList();
        List<DraftAdvice.Combo> composRendues = compos.entrySet().stream()
                .filter(e -> e.getValue().parties >= PIRES_MINIMUM)
                .map(e -> rendu.combo("COMPOSITION", e.getKey(), e.getValue(), TRIO_SEUIL))
                .sorted(Comparator.comparingDouble(DraftAdvice.Combo::lowerBound).reversed())
                .limit(RETENUS)
                .toList();
        List<DraftAdvice.Combo> pires = new ArrayList<>();
        pires.addAll(duosRendus);
        pires.addAll(triosRendus);
        List<DraftAdvice.Combo> aEviter = pires.stream()
                .filter(c -> c.games() >= PIRES_MINIMUM)
                .sorted(Comparator.comparingDouble((DraftAdvice.Combo c) -> borneHaute(c.wins(), c.games())))
                .limit(RETENUS)
                .toList();

        List<DraftAdvice.Combo> affiches = new ArrayList<>();
        DUOS.forEach(type -> duosRendus.stream().filter(c -> c.kind().equals(type.name())).limit(RETENUS)
                .forEach(affiches::add));
        triosRendus.stream().limit(RETENUS).forEach(affiches::add);
        affiches.addAll(aEviter);
        Map<String, GeneralRecord> reperes = reperes(affiches);

        return new DraftAdvice(parties.size(), DUO_SEUIL, TRIO_SEUIL,
                duosRendus.stream().map(c -> avecRepere(c, reperes)).toList(),
                triosRendus.stream().map(c -> avecRepere(c, reperes)).toList(),
                composRendues,
                aEviter.stream().map(c -> avecRepere(c, reperes)).toList(),
                Parallele.attend(essais),
                bans(parties, catalogue),
                Parallele.attend(menaces));
    }

    private static void ajoute(Map<List<Choix>, Compte> comptes, ComboKind type, Map<String, Choix> parPoste,
                               boolean victoire) {
        List<Choix> combo = type.positions().stream().map(parPoste::get).toList();
        if (combo.stream().allMatch(Objects::nonNull)) {
            comptes.computeIfAbsent(combo, c -> new Compte()).ajoute(victoire);
        }
    }

    private static String kindOf(List<Choix> combo) {
        List<String> postes = combo.stream().map(Choix::position).toList();
        return DUOS.stream().filter(type -> type.positions().equals(postes)).map(ComboKind::name).findFirst()
                .orElse("COMPOSITION");
    }

    private record Rendu(Map<String, String> noms, Map<Integer, RiotChampionGateway.Champion> catalogue,
                         Map<Choix, Map<String, Compte>> sansPartenaire) {

        DraftAdvice.Combo combo(String kind, List<Choix> choix, Compte compte, int seuil) {
            return new DraftAdvice.Combo(kind, choix.stream().map(this::pick).toList(), compte.parties,
                    compte.victoires, (double) compte.victoires / compte.parties,
                    borneBasse(compte.victoires, compte.parties), compte.parties < seuil, null, null,
                    attendu(choix));
        }

        DraftAdvice.Pick pick(Choix choix) {
            RiotChampionGateway.Champion champion = catalogue.get(choix.championId());
            return new DraftAdvice.Pick(choix.memberId(), noms.get(choix.memberId()),
                    GameRole.dePosition(choix.position()).orElseThrow().name(), choix.championId(),
                    champion == null ? null : champion.key(), champion == null ? null : champion.name(),
                    champion == null ? null : champion.iconUrl());
        }

        // Moyenne, sur les joueurs du combo, de leur taux sur ce champion quand un autre membre du combo manque.
        Double attendu(List<Choix> choix) {
            List<Double> taux = new ArrayList<>();
            for (Choix un : choix) {
                long parties = 0;
                long victoires = 0;
                for (Choix autre : choix) {
                    if (autre == un) {
                        continue;
                    }
                    Compte sans = sansPartenaire.getOrDefault(un, Map.of()).get(autre.memberId());
                    if (sans != null) {
                        parties += sans.parties;
                        victoires += sans.victoires;
                    }
                }
                if (parties < ATTENDU_MINIMUM) {
                    return null;
                }
                taux.add((double) victoires / parties);
            }
            return taux.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
        }
    }

    record GeneralRecord(long games, long wins) {
    }

    private static String cle(String kind, List<Integer> champions) {
        return kind + champions;
    }

    private Map<String, GeneralRecord> reperes(List<DraftAdvice.Combo> affiches) {
        Map<String, GeneralRecord> reperes = new HashMap<>();
        Map<String, List<DraftAdvice.Combo>> parType = affiches.stream()
                .collect(Collectors.groupingBy(DraftAdvice.Combo::kind, LinkedHashMap::new, Collectors.toList()));
        Parallele.parCle(parType.keySet(), kind -> {
            List<DraftAdvice.Combo> liste = parType.get(kind);
            List<String> postes = liste.get(0).picks().stream()
                    .map(pick -> GameRole.valueOf(pick.role()).position()).toList();
            List<RiotComboGateway.Slot> slots = new ArrayList<>();
            for (int i = 0; i < postes.size(); i++) {
                int index = i;
                slots.add(new RiotComboGateway.Slot(postes.get(i), liste.stream()
                        .map(c -> c.picks().get(index).championId()).distinct().toList()));
            }
            return combos.combos(slots, 1).orElseGet(List::of);
        }).forEach((kind, generaux) -> generaux.forEach(general ->
                reperes.put(cle(kind, general.championIds()), new GeneralRecord(general.games(), general.wins()))));
        return reperes;
    }

    private static DraftAdvice.Combo avecRepere(DraftAdvice.Combo c, Map<String, GeneralRecord> reperes) {
        GeneralRecord repere = reperes.get(cle(c.kind(), c.picks().stream().map(DraftAdvice.Pick::championId)
                .toList()));
        if (repere == null || repere.games() == 0) {
            return c;
        }
        return new DraftAdvice.Combo(c.kind(), c.picks(), c.games(), c.wins(), c.winRate(), c.lowerBound(),
                c.lowConfidence(), repere.games(), (double) repere.wins() / repere.games(), c.expected());
    }

    // #108 : forts en général, dans le pool ou l'historique de chaque joueur, et presque jamais joués ensemble.
    private List<DraftAdvice.Combo> aEssayer(Team team, List<TeamMember> joueurs, Map<String, TeamMember> parPuuid,
                                                Map<String, String> noms,
                                                Map<Integer, RiotChampionGateway.Champion> catalogue,
                                                Map<List<Choix>, Compte> dejaJoues) {
        TeamChampionPool pool = pools.findById(team.getId()).orElseGet(() -> TeamChampionPool.depart(team.getId()));
        Map<String, Integer> parCle = new HashMap<>();
        catalogue.values().forEach(champion -> parCle.put(champion.key(), champion.id()));
        Map<String, Map<Integer, Long>> jouesParMembre = new HashMap<>();
        statsGateway.aggregate(List.copyOf(parPuuid.keySet()), RiotStatsGateway.Grouping.CHAMPION,
                        RiotStatsGateway.Scope.RIFT, null).orElseGet(List::of)
                .forEach(bucket -> {
                    TeamMember membre = parPuuid.get(bucket.puuid());
                    Integer id = entier(bucket.key());
                    if (membre != null && id != null) {
                        jouesParMembre.computeIfAbsent(membre.getMemberId(), m -> new HashMap<>())
                                .put(id, bucket.games());
                    }
                });
        Map<String, Long> ensemble = new HashMap<>();
        dejaJoues.forEach((choix, compte) -> ensemble.merge(
                cle(kindOf(choix), choix.stream().map(Choix::championId).toList()), compte.parties, Long::sum));

        Rendu rendu = new Rendu(noms, catalogue, Map.of());
        Map<ComboKind, List<Map<Integer, String>>> candidatsParType = new LinkedHashMap<>();
        for (ComboKind type : DUOS) {
            // Pour chaque poste : quel membre peut prendre quel champion.
            List<Map<Integer, String>> candidats = new ArrayList<>();
            for (String position : type.positions()) {
                GameRole role = GameRole.dePosition(position).orElseThrow();
                Map<Integer, String> parChampion = new LinkedHashMap<>();
                for (TeamMember membre : joueurs) {
                    if (membre.getRoles() == null || !membre.getRoles().contains(role)) {
                        continue;
                    }
                    pool.championKeys(role).stream().map(parCle::get).filter(Objects::nonNull)
                            .forEach(id -> parChampion.putIfAbsent(id, membre.getMemberId()));
                    jouesParMembre.getOrDefault(membre.getMemberId(), Map.of()).forEach((id, parties) -> {
                        if (parties >= JOUE_MINIMUM) {
                            parChampion.putIfAbsent(id, membre.getMemberId());
                        }
                    });
                }
                candidats.add(parChampion);
            }
            if (candidats.stream().noneMatch(Map::isEmpty)) {
                candidatsParType.put(type, candidats);
            }
        }
        Map<ComboKind, List<RiotComboGateway.GeneralCombo>> generaux = Parallele.parCle(candidatsParType.keySet(),
                type -> {
                    List<Map<Integer, String>> candidats = candidatsParType.get(type);
                    List<RiotComboGateway.Slot> slots = new ArrayList<>();
                    for (int i = 0; i < type.positions().size(); i++) {
                        slots.add(new RiotComboGateway.Slot(type.positions().get(i),
                                List.copyOf(candidats.get(i).keySet())));
                    }
                    return combos.combos(slots, REPERE_SOLIDE).orElseGet(List::of);
                });
        List<DraftAdvice.Combo> propositions = new ArrayList<>();
        for (ComboKind type : candidatsParType.keySet()) {
            List<Map<Integer, String>> candidats = candidatsParType.get(type);
            generaux.get(type).stream()
                    .filter(general -> (double) general.wins() / general.games() > 0.5)
                    .filter(general -> ensemble.getOrDefault(cle(type.name(), general.championIds()), 0L) < DEJA_ESSAYE)
                    .filter(general -> !Objects.equals(candidats.get(0).get(general.championIds().get(0)),
                            candidats.get(1).get(general.championIds().get(1))))
                    .sorted(Comparator.comparingDouble((RiotComboGateway.GeneralCombo g) ->
                            borneBasse(g.wins(), g.games())).reversed())
                    .limit(RETENUS)
                    .forEach(general -> {
                        List<Choix> choix = new ArrayList<>();
                        for (int i = 0; i < type.positions().size(); i++) {
                            int id = general.championIds().get(i);
                            choix.add(new Choix(candidats.get(i).get(id), type.positions().get(i), id));
                        }
                        long joues = ensemble.getOrDefault(cle(type.name(), general.championIds()), 0L);
                        propositions.add(new DraftAdvice.Combo(type.name(),
                                choix.stream().map(rendu::pick).toList(), joues, 0, 0,
                                borneBasse(general.wins(), general.games()), true, general.games(),
                                (double) general.wins() / general.games(), null));
                    });
        }
        return propositions;
    }

    // #106 : impact = présence × perte de taux de victoire, la perte prise à sa borne basse.
    private static List<DraftAdvice.Ban> bans(List<RiotStatsGateway.SharedMatch> parties,
                                                 Map<Integer, RiotChampionGateway.Champion> catalogue) {
        if (parties.isEmpty()) {
            return List.of();
        }
        long victoires = parties.stream().filter(p -> Boolean.TRUE.equals(p.win())).count();
        double moyenne = (double) victoires / parties.size();
        Map<Integer, Compte> face = new HashMap<>();
        for (RiotStatsGateway.SharedMatch partie : parties) {
            VueDePartie.de(partie).adverses().stream().map(RiotStatsGateway.SharedMatchPlayer::championId)
                    .distinct()
                    .forEach(id -> face.computeIfAbsent(id, c -> new Compte()).ajoute(Boolean.TRUE.equals(partie.win())));
        }
        return face.entrySet().stream()
                .filter(e -> e.getValue().parties >= PIRES_MINIMUM)
                .map(e -> {
                    Compte c = e.getValue();
                    double presence = (double) c.parties / parties.size();
                    double taux = (double) c.victoires / c.parties;
                    double perte = Math.max(0, moyenne - borneHaute(c.victoires, c.parties));
                    RiotChampionGateway.Champion champion = catalogue.get(e.getKey());
                    return new DraftAdvice.Ban(e.getKey(), champion == null ? null : champion.key(),
                            champion == null ? null : champion.name(), champion == null ? null : champion.iconUrl(),
                            c.parties, c.victoires, presence, taux, presence * perte, c.parties < DUO_SEUIL);
                })
                .filter(ban -> ban.winRateAgainst() < moyenne)
                // À impact nul (échantillon trop court pour la borne), le taux brut départage.
                .sorted(Comparator.comparingDouble(DraftAdvice.Ban::impact)
                        .thenComparingDouble(ban -> ban.presence() * (moyenne - ban.winRateAgainst()))
                        .reversed())
                .limit(BANS)
                .toList();
    }

    private List<DraftAdvice.LaneThreat> menacesDeCouloir(List<RiotStatsGateway.SharedMatch> parties,
                                                             Map<String, TeamMember> parPuuid,
                                                             Map<String, String> noms,
                                                             Map<Integer, RiotChampionGateway.Champion> catalogue) {
        List<String> ids = parties.stream().map(RiotStatsGateway.SharedMatch::matchId).toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<String, double[]> ecarts = new LinkedHashMap<>();
        for (RiotStatsGateway.Insight insight : statsGateway.insights(ids).orElseGet(List::of)) {
            if (insight.participants() == null) {
                continue;
            }
            for (RiotStatsGateway.InsightPlayer nous : insight.participants()) {
                TeamMember membre = parPuuid.get(nous.puuid());
                if (membre == null) {
                    continue;
                }
                VueDePartie.visAVis(nous, insight.participants()).ifPresent(eux -> {
                    Integer or = VueDePartie.ecartA15(nous, eux, RiotStatsGateway.At15::gold);
                    if (or != null) {
                        double[] somme = ecarts.computeIfAbsent(
                                membre.getMemberId() + "|" + nous.position() + "|" + eux.championId(),
                                k -> new double[2]);
                        somme[0]++;
                        somme[1] += or;
                    }
                });
            }
        }
        return ecarts.entrySet().stream()
                .filter(e -> e.getValue()[0] >= COUP_DE_COULOIR_MINIMUM
                        && e.getValue()[1] / e.getValue()[0] <= RETARD_SIGNIFICATIF)
                .map(e -> {
                    String[] parts = e.getKey().split("\\|");
                    int championId = Integer.parseInt(parts[2]);
                    RiotChampionGateway.Champion champion = catalogue.get(championId);
                    return new DraftAdvice.LaneThreat(parts[0], noms.get(parts[0]),
                            GameRole.dePosition(parts[1]).orElseThrow().name(),
                            championId, champion == null ? null : champion.key(),
                            champion == null ? null : champion.name(), champion == null ? null : champion.iconUrl(),
                            (long) e.getValue()[0], e.getValue()[1] / e.getValue()[0]);
                })
                .sorted(Comparator.comparingDouble(DraftAdvice.LaneThreat::goldDiffAt15))
                .limit(BANS)
                .toList();
    }

    private static Integer entier(String valeur) {
        try {
            return valeur == null ? null : Integer.valueOf(valeur);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // Borne basse de Wilson à 95 % : un 3/3 ne passe pas devant un 15/20.
    static double borneBasse(long victoires, long parties) {
        return wilson(victoires, parties, -1);
    }

    static double borneHaute(long victoires, long parties) {
        return wilson(victoires, parties, 1);
    }

    private static double wilson(long victoires, long parties, int signe) {
        if (parties == 0) {
            return signe < 0 ? 0 : 1;
        }
        double p = (double) victoires / parties;
        double z2 = Z * Z;
        double centre = p + z2 / (2.0 * parties);
        double marge = Z * Math.sqrt(p * (1 - p) / parties + z2 / (4.0 * parties * parties));
        return (centre + signe * marge) / (1 + z2 / parties);
    }
}
