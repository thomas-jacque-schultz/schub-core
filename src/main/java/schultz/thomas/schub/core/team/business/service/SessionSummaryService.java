package schultz.thomas.schub.core.team.business.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import schultz.thomas.schub.core.business.service.PremadeLabGateway.Embed;
import schultz.thomas.schub.core.business.service.PremadeLabGateway;
import schultz.thomas.schub.core.data.model.SessionSummaryPreferences;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.data.repository.UserRepository;
import schultz.thomas.schub.core.team.business.model.GameModes;
import schultz.thomas.schub.core.team.data.model.GameRole;
import schultz.thomas.schub.core.team.data.model.Team;
import schultz.thomas.schub.core.team.data.model.TeamMember;
import schultz.thomas.schub.core.team.data.model.TeamSession;
import schultz.thomas.schub.core.team.data.repository.TeamRepository;
import schultz.thomas.schub.core.team.data.repository.TeamSessionRepository;

import java.time.Instant;
import java.time.LocalDate;
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

// Le résumé de session en MP, le lendemain (Schub#99). Un MP par personne et par jour, aux seuls présents ;
// planifiées activées par défaut, libres désactivées (réponse du 05/10).
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionSummaryService {

    static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH);
    static final DateTimeFormatter HEURE = DateTimeFormatter.ofPattern("H' h 'mm", Locale.FRENCH);
    // Un écart de moins de 10 % à sa propre moyenne n'est pas un constat.
    static final double ECART_NOTABLE = 0.10;
    static final int AVANCE_A_15 = 1000;
    static final int REFERENCE_JOURS = 90;
    static final int OR_PREMADELAB = 0xC9A227;
    // Limites de Discord : 10 encadrés et 6 000 caractères par message, 25 champs par encadré.
    static final int ENCADRES_MAX = 10;
    static final int CARACTERES_MAX = 6000;
    static final int PARTIES_MAX = 24;

    private final TeamSessionRepository sessions;
    private final TeamRepository teams;
    private final UserRepository users;
    private final RiotStatsGateway statsGateway;
    private final RiotChampionGateway championGateway;
    private final TeamGamesStatsService gamesStats;
    private final PremadeLabGateway premadelab;

    @Value("${premadelab.url:https://premadelab.eu}")
    private String premadelabUrl;

    // 8 h : après la détection de 6 h.
    @Scheduled(cron = "0 0 8 * * *", zone = "Europe/Paris")
    public void sendYesterday() {
        send(LocalDate.now(PARIS).minusDays(1));
    }

    public void send(LocalDate jour) {
        Map<String, List<TeamSession>> parMembre = presences(jour);
        Map<String, Team> equipes = new LinkedHashMap<>();
        for (Map.Entry<String, List<TeamSession>> entree : parMembre.entrySet()) {
            String userId = entree.getKey().split("\\|")[0];
            Optional<User> user = users.findById(userId);
            if (user.isEmpty() || user.get().getDiscordId() == null
                    || jour.toString().equals(user.get().getLastSessionSummaryDay())) {
                continue;
            }
            List<Embed> encadres = limite(voulues(user.get(), entree.getValue()).stream()
                    .map(s -> compose(equipes.computeIfAbsent(s.teamId(), id -> teams.findById(id).orElse(null)),
                            s, entree.getKey().split("\\|")[1]))
                    .filter(Objects::nonNull)
                    .toList());
            if (encadres.isEmpty()) {
                continue;
            }
            PremadeLabGateway.Status statut =
                    premadelab.sendDirectMessage(user.get().getDiscordId(), encadres).status();
            switch (statut) {
                case SENT -> {
                    user.get().setLastSessionSummaryDay(jour.toString());
                    user.get().setDmUnreachableAt(null);
                    users.save(user.get());
                }
                case UNREACHABLE -> {
                    user.get().setDmUnreachableAt(Instant.now());
                    users.save(user.get());
                }
                case DISABLED, FAILED -> log.info("Résumé du {} non envoyé à {} : {}", jour, userId, statut);
            }
        }
    }

    // Ce que recevrait ce compte pour ce jour, sans rien envoyer.
    public Optional<List<Embed>> preview(User user, LocalDate jour) {
        return encadres(user, jour, true);
    }

    public record RecentSession(LocalDate day, Instant startedAt, String teamName, long games, long wins,
                                boolean planned) {
    }

    public record TestResult(List<Embed> embeds, PremadeLabGateway.Status status, String detail) {
    }

    public List<RecentSession> recentFor(User user, int jours) {
        Map<String, Optional<Team>> equipes = new LinkedHashMap<>();
        return sessions.findByDayGreaterThanEqualOrderByStartedAtDesc(LocalDate.now(PARIS).minusDays(jours)).stream()
                .filter(s -> !s.missed())
                .flatMap(s -> equipes.computeIfAbsent(s.teamId(), teams::findById).stream()
                        .filter(team -> team.getMembers().stream().anyMatch(m -> user.getId().equals(m.getUserId())
                                && s.memberIds().contains(m.getMemberId())))
                        .map(team -> new RecentSession(s.day(), s.startedAt(), team.getName(), s.games(), s.wins(),
                                s.planned())))
                .toList();
    }

    // Debug : ni préférences, ni envoi unique par jour, et l'envoi n'est pas noté sur le compte.
    public Optional<TestResult> sendTest(User user, LocalDate jour, boolean envoyer) {
        return encadres(user, jour, false).map(encadres -> {
            if (!envoyer) {
                return new TestResult(encadres, null, null);
            }
            if (user.getDiscordId() == null) {
                return new TestResult(encadres, PremadeLabGateway.Status.DISABLED,
                        "Aucun compte Discord lié");
            }
            PremadeLabGateway.Result envoi = premadelab.sendDirectMessage(user.getDiscordId(), encadres);
            return new TestResult(encadres, envoi.status(), envoi.detail());
        });
    }

    private Optional<List<Embed>> encadres(User user, LocalDate jour, boolean selonPreferences) {
        List<Embed> encadres = new ArrayList<>();
        presences(jour).forEach((cle, liste) -> {
            if (cle.startsWith(user.getId() + "|")) {
                (selonPreferences ? voulues(user, liste) : liste).forEach(s -> teams.findById(s.teamId())
                        .map(team -> compose(team, s, cle.split("\\|")[1]))
                        .ifPresent(encadres::add));
            }
        });
        return encadres.isEmpty() ? Optional.empty() : Optional.of(limite(encadres));
    }

    static List<Embed> limite(List<Embed> encadres) {
        List<Embed> retenus = new ArrayList<>();
        int total = 0;
        for (Embed encadre : encadres) {
            total += taille(encadre);
            if (retenus.size() == ENCADRES_MAX || (!retenus.isEmpty() && total > CARACTERES_MAX)) {
                break;
            }
            retenus.add(encadre);
        }
        return retenus;
    }

    private static int taille(Embed encadre) {
        int taille = longueur(encadre.title()) + longueur(encadre.description())
                + (encadre.footer() == null ? 0 : longueur(encadre.footer().text()));
        for (Embed.Field champ : encadre.fields()) {
            taille += longueur(champ.name()) + longueur(champ.value());
        }
        return taille;
    }

    private static int longueur(String texte) {
        return texte == null ? 0 : texte.length();
    }

    private static List<TeamSession> voulues(User user, List<TeamSession> liste) {
        SessionSummaryPreferences prefs = SessionSummaryPreferences.of(user);
        return liste.stream().filter(s -> s.planned() ? prefs.planned() : prefs.free()).toList();
    }

    // Clé « userId|memberId » : les présents seulement, un absent ne reçoit rien (réponse du 05/10).
    private Map<String, List<TeamSession>> presences(LocalDate jour) {
        Map<String, List<TeamSession>> parMembre = new LinkedHashMap<>();
        for (TeamSession session : sessions.findByDay(jour)) {
            if (session.missed()) {
                continue;
            }
            teams.findById(session.teamId()).ifPresent(team -> team.getMembers().stream()
                    .filter(m -> m.getUserId() != null && session.memberIds().contains(m.getMemberId()))
                    .forEach(m -> parMembre.computeIfAbsent(m.getUserId() + "|" + m.getMemberId(),
                            k -> new ArrayList<>()).add(session)));
        }
        return parMembre;
    }

    // Une ligne par partie (poste, champions, début de partie, écart final), puis le bilan de la session.
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

        List<Embed.Field> champs = new ArrayList<>();
        parties.stream().limit(PARTIES_MAX).forEach(partie -> champs.add(
                partie(partie, monPuuid, insights.get(partie.matchId()), champion)));
        champs.add(new Embed.Field("Bilan", bilan(team, session, monPuuid, parties, parPuuid, noms, insights), false));

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
        String poste = moi == null ? null : poste(moi);
        RiotStatsGateway.SharedMatchPlayer adverse = adverse(partie, moi);
        String resultat = partie.win() == null ? "➖" : partie.win() ? "✅ Victoire" : "❌ Défaite";
        String contexte = moi == null ? "sans toi"
                : poste != null ? poste : GameModes.libelleHorsFaille(partie.queue()).orElse(null);
        String nom = resultat + (contexte == null ? "" : " · " + contexte) + " · "
                + partie.durationSeconds() / 60 + " min";

        List<String> lignes = new ArrayList<>();
        if (moi != null) {
            lignes.add(champion.apply(moi) + (adverse == null ? "" : " contre " + champion.apply(adverse))
                    + " · " + moi.kills() + "/" + moi.deaths() + "/" + moi.assists());
        }
        RiotStatsGateway.InsightPlayer nous = a15(insight, moi);
        RiotStatsGateway.InsightPlayer eux = a15(insight, adverse);
        Integer or = VueDePartie.ecartA15(nous, eux, RiotStatsGateway.At15::gold);
        if (or != null) {
            lignes.add("À 15 min : " + signe(or) + " or et "
                    + signe(VueDePartie.ecartA15(nous, eux, RiotStatsGateway.At15::cs)) + " CS sur lui, "
                    + nous.at15().kills() + "/" + nous.at15().deaths() + "/" + nous.at15().assists());
        }
        Integer ecart = VueDePartie.de(partie).ecartDOrFinal();
        if (ecart != null) {
            lignes.add("Résultat équipe : " + signe(ecart) + " or en fin de partie");
        }
        return new Embed.Field(nom, lignes.isEmpty() ? "—" : String.join("\n", lignes), false);
    }

    static String sansTag(String riotId) {
        return riotId == null || riotId.indexOf("#") <= 0 ? riotId : riotId.substring(0, riotId.indexOf("#"));
    }

    private String bilan(Team team, TeamSession session, String monPuuid, List<RiotStatsGateway.SharedMatch> parties,
                         Map<String, TeamMember> parPuuid, Map<String, String> noms,
                         Map<String, RiotStatsGateway.Insight> insights) {
        long secondes = parties.stream().mapToLong(RiotStatsGateway.SharedMatch::durationSeconds).sum();
        List<String> lignes = new ArrayList<>();
        lignes.add("**" + session.wins() + " V – " + (session.games() - session.wins()) + " D** en "
                + session.games() + " parties · " + duree(secondes) + " de jeu");
        if (monPuuid != null) {
            List<RiotStatsGateway.SharedMatchPlayer> miennes = parties.stream().map(p -> moi(p, monPuuid))
                    .filter(Objects::nonNull).toList();
            if (!miennes.isEmpty()) {
                lignes.add("Postes : " + postes(parties, monPuuid));
            }
            lignes.addAll(toi(monPuuid, session, miennes));
        }
        lignes.addAll(equipe(team, session, parties, parPuuid, noms, insights));
        return String.join("\n", lignes);
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
                                Map<String, TeamMember> parPuuid, Map<String, String> noms,
                                Map<String, RiotStatsGateway.Insight> insights) {
        List<String> lignes = new ArrayList<>();

        // Le duo du soir : la paire qui a gagné le plus de parties ensemble.
        Map<String, long[]> duos = new LinkedHashMap<>();
        for (RiotStatsGateway.SharedMatch partie : parties) {
            List<String> presents = partie.players().stream().map(j -> parPuuid.get(j.puuid()))
                    .filter(Objects::nonNull).map(TeamMember::getMemberId).sorted().toList();
            for (int i = 0; i < presents.size(); i++) {
                for (int j = i + 1; j < presents.size(); j++) {
                    long[] c = duos.computeIfAbsent(presents.get(i) + "|" + presents.get(j), k -> new long[2]);
                    c[0]++;
                    c[1] += Boolean.TRUE.equals(partie.win()) ? 1 : 0;
                }
            }
        }
        duos.entrySet().stream()
                .filter(e -> e.getValue()[1] >= 2)
                .max(Comparator.comparingLong((Map.Entry<String, long[]> e) -> e.getValue()[1])
                        .thenComparingLong(e -> -e.getValue()[0]))
                .ifPresent(e -> {
                    String[] paire = e.getKey().split("\\|");
                    lignes.add("Duo du soir : " + sansTag(noms.get(paire[0])) + " et " + sansTag(noms.get(paire[1]))
                            + ", " + e.getValue()[1] + " V – " + (e.getValue()[0] - e.getValue()[1]) + " D");
                });

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

        if (session.planned()) {
            sessions.findByTeamIdAndDayGreaterThanEqualOrderByStartedAtDesc(team.getId(),
                            session.day().minusDays(REFERENCE_JOURS)).stream()
                    .filter(s -> s.planned() && !s.missed() && s.startedAt().isBefore(session.startedAt()))
                    .findFirst()
                    .ifPresent(avant -> lignes.add("Séance précédente : "
                            + Math.round(100.0 * avant.wins() / Math.max(1, avant.games())) + " % de victoires, "
                            + Math.round(100.0 * session.wins() / session.games()) + " % ce soir"));
        }
        return lignes;
    }
}
