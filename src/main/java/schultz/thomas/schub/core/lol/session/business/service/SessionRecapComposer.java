package schultz.thomas.schub.core.lol.session.business.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import schultz.thomas.schub.core.business.service.PremadeLabGateway;
import schultz.thomas.schub.core.business.service.PremadeLabGateway.Embed;
import schultz.thomas.schub.core.lol.riot.business.model.GameModes;
import schultz.thomas.schub.core.lol.riot.business.service.RiotChampionGateway;
import schultz.thomas.schub.core.lol.riot.business.service.RiotStatsGateway;
import schultz.thomas.schub.core.lol.riot.business.service.VueDePartie;
import schultz.thomas.schub.core.lol.session.data.model.TeamSession;
import schultz.thomas.schub.core.lol.session.data.repository.TeamSessionRepository;
import schultz.thomas.schub.core.lol.stats.business.service.TeamGamesStatsService;
import schultz.thomas.schub.core.lol.stats.business.service.TeamPlayerStatsService;
import schultz.thomas.schub.core.lol.team.data.model.GameRole;
import schultz.thomas.schub.core.lol.team.data.model.Team;
import schultz.thomas.schub.core.lol.team.data.model.TeamMember;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.stream.Stream;

// L'encadré Discord d'une session, vu par un membre (Schub#99).
@Component
@RequiredArgsConstructor
class SessionRecapComposer {

    static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH);
    static final DateTimeFormatter HEURE = DateTimeFormatter.ofPattern("H' h 'mm", Locale.FRENCH);
    // Un écart de moins de 10 % à sa propre moyenne n'est pas un constat.
    static final double ECART_NOTABLE = 0.10;
    static final int AVANCE_A_15 = 1000;
    static final int REFERENCE_JOURS = 90;
    // En deçà, le vis-à-vis n'a pas pris d'avance qui mérite d'en reparler.
    static final int RETARD_A_REVOIR = 500;
    static final int HISTORIQUE_JOURS = 366;
    static final int HISTORIQUE_MAX = 10;
    static final String BARRES = "▁▂▃▄▅▆▇█";
    // Discord refuse un message de plus de 6 000 caractères : le bilan et le titre gardent le reste.
    static final int DETAIL_MAX = 4500;
    static final int OR_PREMADELAB = 0xC9A227;
    static final int PARTIES_MAX = 24;

    private final TeamSessionRepository sessions;
    private final RiotStatsGateway statsGateway;
    private final RiotChampionGateway championGateway;
    private final TeamGamesStatsService gamesStats;
    private final PremadeLabGateway premadelab;

    @Value("${premadelab.url:https://premadelab.eu}")
    private String premadelabUrl;

    // Une ligne par partie jouée (poste, champions, comparatifs, lane à revoir), puis le bilan de la session.
    Embed compose(Team team, TeamSession session, String memberId) {
        if (team == null) {
            return null;
        }
        List<TeamMember> joueurs = TeamPlayerStatsService.joueursDe(team);
        Map<String, String> noms = gamesStats.noms(joueurs);
        Map<String, TeamMember> parPuuid = TeamPlayerStatsService.parPuuid(joueurs);
        TeamMember moi = joueurs.stream().filter(m -> memberId.equals(m.getMemberId())).findFirst().orElse(null);
        String monPuuid = moi == null ? null : moi.getRiotPuuid();
        List<RiotStatsGateway.SharedMatch> parties = statsGateway.sharedMatchesAmong(List.copyOf(parPuuid.keySet()),
                        SessionWatcher.MEMBRES_MINIMUM, session.matchIds())
                .map(RiotStatsGateway.SharedMatches::matches).orElseGet(List::of).stream()
                .sorted(Comparator.comparing(RiotStatsGateway.SharedMatch::startedAt))
                .toList();
        Map<String, RiotStatsGateway.Insight> insights = new HashMap<>();
        statsGateway.insights(session.matchIds()).orElseGet(List::of)
                .forEach(insight -> insights.put(insight.matchId(), insight));

        Function<RiotStatsGateway.SharedMatchPlayer, String> champion = icones(parties, monPuuid);

        List<RiotStatsGateway.SharedMatch> jouees = parties.stream().filter(p -> moi(p, monPuuid) != null).toList();
        List<Embed.Field> champs = new ArrayList<>();
        int reste = DETAIL_MAX;
        for (RiotStatsGateway.SharedMatch partie : jouees.stream().limit(PARTIES_MAX).toList()) {
            Embed.Field champ = partie(partie, monPuuid, insights.get(partie.matchId()), champion);
            reste -= champ.name().length() + champ.value().length();
            if (reste < 0) {
                break;
            }
            champs.add(champ);
        }
        champs.add(new Embed.Field("Bilan", bilan(team, session, monPuuid, parties, insights,
                jouees.size() - champs.size()), false));

        List<String> avec = joueurs.stream()
                .filter(m -> session.memberIds().contains(m.getMemberId()) && !memberId.equals(m.getMemberId()))
                .map(m -> sansTag(noms.get(m.getMemberId())))
                .filter(Objects::nonNull)
                .toList();
        String jour = session.startedAt().atZone(PARIS).format(JOUR);
        return new Embed(
                (session.planned() ? "Séance d'entraînement du " : "Session du ") + jour + " · " + team.getName(),
                premadelabUrl + "/teams/" + team.getId(),
                "De " + session.startedAt().atZone(PARIS).format(HEURE) + " à "
                        + session.endedAt().atZone(PARIS).format(HEURE)
                        + (avec.isEmpty() ? "" : ", avec " + String.join(", ", avec)),
                OR_PREMADELAB,
                champs,
                new Embed.Footer("PremadeLab"));
    }

    // Les icônes de mes champions et de mes vis-à-vis ; sans emoji, le nom en gras.
    private Function<RiotStatsGateway.SharedMatchPlayer, String> icones(List<RiotStatsGateway.SharedMatch> parties,
                                                                      String monPuuid) {
        Map<Integer, RiotChampionGateway.Champion> catalogue = championGateway.catalogue()
                .map(RiotChampionGateway.Catalogue::parId).orElseGet(Map::of);
        List<PremadeLabGateway.ChampionIcon> demandes = parties.stream()
                .flatMap(p -> {
                    RiotStatsGateway.SharedMatchPlayer moi = moi(p, monPuuid);
                    RiotStatsGateway.SharedMatchPlayer adverse = adverse(p, moi);
                    return Stream.of(moi, adverse);
                })
                .filter(Objects::nonNull)
                .map(j -> catalogue.get(j.championId()))
                .filter(c -> c != null && c.key() != null && c.iconUrl() != null)
                .distinct()
                .map(c -> new PremadeLabGateway.ChampionIcon(c.key(), c.iconUrl()))
                .toList();
        Map<String, String> emojis = premadelab.championEmojis(demandes);
        return joueur -> {
            RiotChampionGateway.Champion c = catalogue.get(joueur.championId());
            String emoji = c == null ? null : emojis.get(c.key());
            return emoji != null ? emoji : "**" + joueur.championName() + "**";
        };
    }

    private static RiotStatsGateway.SharedMatchPlayer moi(RiotStatsGateway.SharedMatch partie, String monPuuid) {
        return partie.players().stream().filter(j -> j.puuid().equals(monPuuid)).findFirst().orElse(null);
    }

    private static RiotStatsGateway.SharedMatchPlayer adverse(RiotStatsGateway.SharedMatch partie,
                                                              RiotStatsGateway.SharedMatchPlayer moi) {
        return VueDePartie.visAVis(moi, partie.others()).orElse(null);
    }

    private static String poste(RiotStatsGateway.SharedMatchPlayer joueur) {
        return GameRole.dePosition(joueur.position()).map(GameRole::libelle).orElse(null);
    }

    private static RiotStatsGateway.InsightPlayer a15(RiotStatsGateway.Insight insight,
                                                      RiotStatsGateway.SharedMatchPlayer joueur) {
        return insight == null || insight.participants() == null || joueur == null ? null
                : insight.participants().stream()
                .filter(j -> j.at15() != null && j.puuid().equals(joueur.puuid()))
                .findFirst().orElse(null);
    }

    private static Embed.Field partie(RiotStatsGateway.SharedMatch partie, String monPuuid,
                                      RiotStatsGateway.Insight insight,
                                      Function<RiotStatsGateway.SharedMatchPlayer, String> champion) {
        RiotStatsGateway.SharedMatchPlayer moi = moi(partie, monPuuid);
        RiotStatsGateway.SharedMatchPlayer adverse = adverse(partie, moi);
        String resultat = partie.win() == null ? "➖" : partie.win() ? "✅ Victoire" : "❌ Défaite";
        String poste = poste(moi);
        String contexte = poste != null ? poste : GameModes.libelleHorsFaille(partie.queue()).orElse(null);
        String nom = resultat + (contexte == null ? "" : " · " + contexte) + " · "
                + partie.durationSeconds() / 60 + " min";

        List<String> lignes = new ArrayList<>();
        lignes.add(adverse == null ? champion.apply(moi) + " · " + score(moi.kills(), moi.deaths(), moi.assists())
                : champion.apply(moi) + " contre " + champion.apply(adverse));
        RiotStatsGateway.InsightPlayer nous = a15(insight, moi);
        RiotStatsGateway.InsightPlayer eux = a15(insight, adverse);
        Integer or = VueDePartie.ecartA15(nous, eux, RiotStatsGateway.At15::gold);
        if (or != null) {
            lignes.add("À 15 min : " + comparatif(or, VueDePartie.ecartA15(nous, eux, RiotStatsGateway.At15::cs),
                    nous.at15().kills(), nous.at15().deaths(), nous.at15().assists()));
        }
        if (adverse != null) {
            lignes.add("Fin de partie : " + comparatif(moi.goldEarned() - adverse.goldEarned(),
                    moi.minionsKilled() - adverse.minionsKilled(), moi.kills(), moi.deaths(), moi.assists()));
        }
        Integer ecart = VueDePartie.de(partie).ecartDOrFinal();
        if (ecart != null) {
            lignes.add("Résultat équipe : " + signe(ecart) + " or en fin de partie");
        }
        laneAReviser(insight, moi.side()).ifPresent(lignes::add);
        return new Embed.Field(nom, String.join("\n", lignes), false);
    }

    private static String comparatif(int or, int cs, int kills, int deaths, int assists) {
        return signe(or) + " or et " + signe(cs) + " CS sur lui, " + score(kills, deaths, assists);
    }

    private static String score(int kills, int deaths, int assists) {
        return kills + "/" + deaths + "/" + assists;
    }

    // Le poste de notre camp le plus en retard à 15 min, sans le nom de celui qui l'a joué : on revoit une lane à
    // plusieurs, on ne désigne personne.
    static Optional<String> laneAReviser(RiotStatsGateway.Insight insight, int cote) {
        if (insight == null || insight.participants() == null) {
            return Optional.empty();
        }
        RiotStatsGateway.InsightPlayer lane = null;
        int retard = -RETARD_A_REVOIR;
        for (RiotStatsGateway.InsightPlayer joueur : VueDePartie.camp(insight.participants(), cote, true)) {
            Integer ecart = VueDePartie.visAVis(joueur, insight.participants())
                    .map(vis -> VueDePartie.ecartA15(joueur, vis, RiotStatsGateway.At15::gold))
                    .orElse(null);
            if (ecart != null && ecart <= retard) {
                lane = joueur;
                retard = ecart;
            }
        }
        return lane == null ? Optional.empty()
                : GameRole.dePosition(lane.position()).map(GameRole::libelle).map(
                        libelle -> "Lane pour review d'équipe : " + libelle);
    }

    // Une barre par séance, de la plus ancienne à la plus récente, à la hauteur de son taux de victoires.
    static String histogramme(List<TeamSession> anciennesDAbord) {
        StringBuilder barres = new StringBuilder();
        anciennesDAbord.forEach(s -> barres.append(BARRES.charAt(
                (int) Math.round((BARRES.length() - 1.0) * s.wins() / Math.max(1, s.games())))));
        return barres.toString();
    }

    static String sansTag(String riotId) {
        return riotId == null || riotId.indexOf("#") <= 0 ? riotId : riotId.substring(0, riotId.indexOf("#"));
    }

    private String bilan(Team team, TeamSession session, String monPuuid, List<RiotStatsGateway.SharedMatch> parties,
                         Map<String, RiotStatsGateway.Insight> insights, int nonDetaillees) {
        long secondes = parties.stream().mapToLong(RiotStatsGateway.SharedMatch::durationSeconds).sum();
        List<String> lignes = new ArrayList<>();
        lignes.add("**" + session.wins() + " V – " + (session.games() - session.wins()) + " D** en "
                + session.games() + " parties · " + duree(secondes) + " de jeu");
        List<RiotStatsGateway.SharedMatchPlayer> miennes = parties.stream().map(p -> moi(p, monPuuid))
                .filter(Objects::nonNull).toList();
        int sansToi = parties.size() - miennes.size();
        if (sansToi > 0) {
            lignes.add(parties(sansToi) + " sans toi");
        }
        if (nonDetaillees > 0) {
            lignes.add(parties(nonDetaillees) + " non détaillée" + (nonDetaillees > 1 ? "s" : "") + " ici");
        }
        if (monPuuid != null) {
            if (!miennes.isEmpty()) {
                lignes.add("Postes : " + postes(parties, monPuuid));
            }
            lignes.addAll(toi(monPuuid, session, miennes));
        }
        lignes.addAll(equipe(team, session, parties, insights));
        return String.join("\n", lignes);
    }

    private static String parties(int nombre) {
        return nombre + (nombre > 1 ? " parties" : " partie");
    }

    // « Top ×6, Mid ×1, ARAM ×1 », dans l'ordre du nombre de parties.
    static String postes(List<RiotStatsGateway.SharedMatch> parties, String monPuuid) {
        Map<String, Integer> compte = new LinkedHashMap<>();
        for (RiotStatsGateway.SharedMatch partie : parties) {
            RiotStatsGateway.SharedMatchPlayer moi = moi(partie, monPuuid);
            if (moi != null) {
                String poste = Optional.ofNullable(poste(moi))
                        .or(() -> GameModes.libelleHorsFaille(partie.queue())).orElse("autre mode");
                compte.merge(poste, 1, Integer::sum);
            }
        }
        return String.join(", ", compte.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(e -> e.getKey() + " ×" + e.getValue())
                .toList());
    }

    static String signe(int valeur) {
        int absolu = Math.abs(valeur);
        String chiffre = absolu >= 1000 ? String.format(Locale.FRANCE, "%.1f k", absolu / 1000.0)
                : String.valueOf(absolu);
        return (valeur < 0 ? "−" : "+") + chiffre;
    }

    static String duree(long secondes) {
        long minutes = secondes / 60;
        return minutes >= 60 ? minutes / 60 + " h " + String.format("%02d", minutes % 60) : minutes + " min";
    }

    private record Mesure(String libelle, ToDoubleFunction<RiotStatsGateway.Bucket> valeur, boolean plusHautMieux,
                          String format) {
    }

    private static final Mesure KDA =
            new Mesure("KDA", b -> (b.kills() + b.assists()) / (double) Math.max(1, b.deaths()), true, "%.1f");

    // Le KDA a sa propre ligne : il n'entre pas dans les points forts et faibles.
    private static final List<Mesure> MESURES = List.of(
            new Mesure("CS/min", b -> b.minionsKilled() / minutes(b), true, "%.1f"),
            new Mesure("Or/min", b -> b.goldEarned() / minutes(b), true, "%.0f"),
            new Mesure("Dégâts/min", b -> b.damageToChampions() / minutes(b), true, "%.0f"),
            new Mesure("Vision/min", b -> b.visionScore() / minutes(b), true, "%.2f"),
            new Mesure("Morts par partie", b -> b.deaths() / (double) Math.max(1, b.games()), false, "%.1f"));

    private static double minutes(RiotStatsGateway.Bucket b) {
        return Math.max(1, b.secondsPlayed() / 60.0);
    }

    // Le joueur comparé à lui-même, jamais aux autres : son KDA, son point le plus fort, et le plus faible.
    private List<String> toi(String puuid, TeamSession session, List<RiotStatsGateway.SharedMatchPlayer> miennes) {
        List<String> lignes = new ArrayList<>();
        Optional<RiotStatsGateway.Bucket> soir = statsGateway.aggregate(List.of(puuid),
                        RiotStatsGateway.Grouping.OVERALL, RiotStatsGateway.Scope.RIFT, null, session.matchIds())
                .flatMap(l -> l.stream().findFirst());
        Optional<RiotStatsGateway.Bucket> habitude = statsGateway.aggregate(List.of(puuid),
                        RiotStatsGateway.Grouping.OVERALL, RiotStatsGateway.Scope.RIFT,
                        Instant.now().minus(REFERENCE_JOURS, ChronoUnit.DAYS))
                .flatMap(l -> l.stream().findFirst());
        if (!miennes.isEmpty()) {
            int k = miennes.stream().mapToInt(RiotStatsGateway.SharedMatchPlayer::kills).sum();
            int d = miennes.stream().mapToInt(RiotStatsGateway.SharedMatchPlayer::deaths).sum();
            int a = miennes.stream().mapToInt(RiotStatsGateway.SharedMatchPlayer::assists).sum();
            lignes.add("KDA " + String.format(Locale.FRANCE, KDA.format(), (k + a) / (double) Math.max(1, d))
                    + " · " + k + "/" + d + "/" + a
                    + habitude.map(h -> " (ta moyenne " + String.format(Locale.FRANCE, KDA.format(),
                            KDA.valeur().applyAsDouble(h)) + ")").orElse(""));
        }
        if (soir.isEmpty() || habitude.isEmpty() || soir.get().games() == 0) {
            return lignes;
        }
        record Ecart(Mesure mesure, double soir, double habitude, double relatif) {
        }
        List<Ecart> ecarts = new ArrayList<>();
        for (Mesure mesure : MESURES) {
            double s = mesure.valeur().applyAsDouble(soir.get());
            double h = mesure.valeur().applyAsDouble(habitude.get());
            if (h > 0) {
                double relatif = (s - h) / h * (mesure.plusHautMieux() ? 1 : -1);
                ecarts.add(new Ecart(mesure, s, h, relatif));
            }
        }
        ecarts.stream().filter(e -> e.relatif() >= ECART_NOTABLE).max(Comparator.comparingDouble(Ecart::relatif))
                .ifPresent(e -> lignes.add("✅ " + ligne(e.mesure(), e.soir(), e.habitude())));
        ecarts.stream().filter(e -> e.relatif() <= -ECART_NOTABLE).min(Comparator.comparingDouble(Ecart::relatif))
                .ifPresent(e -> lignes.add("🔧 " + ligne(e.mesure(), e.soir(), e.habitude())));
        return lignes;
    }

    private static String ligne(Mesure mesure, double soir, double habitude) {
        return mesure.libelle() + " " + String.format(Locale.FRANCE, mesure.format(), soir)
                + " (ta moyenne " + String.format(Locale.FRANCE, mesure.format(), habitude) + ")";
    }

    private List<String> equipe(Team team, TeamSession session, List<RiotStatsGateway.SharedMatch> parties,
                                Map<String, RiotStatsGateway.Insight> insights) {
        List<String> lignes = new ArrayList<>();

        long avancesPerdues = parties.stream()
                .filter(p -> Boolean.FALSE.equals(p.win()))
                .map(p -> VueDePartie.ecartDesCampsA15(insights.get(p.matchId()), VueDePartie.de(p).cote(),
                        RiotStatsGateway.At15::gold))
                .filter(avance -> avance != null && avance >= AVANCE_A_15)
                .count();
        if (avancesPerdues > 0) {
            lignes.add(avancesPerdues + (avancesPerdues > 1 ? " parties perdues" : " partie perdue")
                    + " après une avance à 15 min");
        }

        List<TeamSession> dernieres = sessions.findByTeamIdAndDayGreaterThanEqualOrderByStartedAtDesc(team.getId(),
                        session.day().minusDays(HISTORIQUE_JOURS)).stream()
                .filter(s -> s.planned() == session.planned() && !s.missed()
                        && !s.startedAt().isAfter(session.startedAt()))
                .limit(HISTORIQUE_MAX)
                .toList();
        if (session.planned()) {
            dernieres.stream().filter(s -> s.startedAt().isBefore(session.startedAt())).findFirst()
                    .ifPresent(avant -> lignes.add("Séance précédente : "
                            + Math.round(100.0 * avant.wins() / Math.max(1, avant.games())) + " % de victoires, "
                            + Math.round(100.0 * session.wins() / session.games()) + " % ce soir"));
        }
        if (dernieres.size() >= 2) {
            lignes.add(dernieres.size() + (session.planned() ? " dernières séances" : " dernières sessions")
                    + " : `" + histogramme(dernieres.reversed()) + "`");
        }
        return lignes;
    }
}
