package schultz.thomas.schub.core.team.business.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import schultz.thomas.schub.core.business.service.ConnectorPremadeLabGateway;
import schultz.thomas.schub.core.data.model.SessionSummaryPreferences;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.data.repository.UserRepository;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.ToDoubleFunction;

// Le résumé de session en MP, le lendemain (Schub#99). Un MP par personne et par jour, aux seuls présents ;
// planifiées activées par défaut, libres désactivées (réponse du 05/10).
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionSummaryService {

    static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH);
    // Un écart de moins de 10 % à sa propre moyenne n'est pas un constat.
    static final double ECART_NOTABLE = 0.10;
    static final int AVANCE_A_15 = 1000;
    static final int REFERENCE_JOURS = 90;

    private final TeamSessionRepository sessions;
    private final TeamRepository teams;
    private final UserRepository users;
    private final RiotStatsGateway statsGateway;
    private final TeamGamesStatsService gamesStats;
    private final ConnectorPremadeLabGateway premadelab;

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
            List<TeamSession> voulues = voulues(user.get(), entree.getValue());
            if (voulues.isEmpty()) {
                continue;
            }
            String texte = voulues.stream()
                    .map(s -> compose(equipes.computeIfAbsent(s.teamId(), id -> teams.findById(id).orElse(null)),
                            s, entree.getKey().split("\\|")[1]))
                    .filter(Objects::nonNull)
                    .reduce((a, b) -> a + "\n\n" + b).orElse(null);
            if (texte == null) {
                continue;
            }
            ConnectorPremadeLabGateway.Status statut = premadelab.sendDirectMessage(user.get().getDiscordId(),
                    texte.length() > 2000 ? texte.substring(0, 1997) + "…" : texte);
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
    public Optional<String> preview(User user, LocalDate jour) {
        List<String> parties = new ArrayList<>();
        presences(jour).forEach((cle, liste) -> {
            if (cle.startsWith(user.getId() + "|")) {
                voulues(user, liste).forEach(s -> teams.findById(s.teamId())
                        .map(team -> compose(team, s, cle.split("\\|")[1]))
                        .ifPresent(parties::add));
            }
        });
        return parties.isEmpty() ? Optional.empty() : Optional.of(String.join("\n\n", parties));
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

    String compose(Team team, TeamSession session, String memberId) {
        if (team == null) {
            return null;
        }
        List<TeamMember> joueurs = TeamPlayerStatsService.joueursDe(team);
        Map<String, String> noms = gamesStats.noms(joueurs);
        TeamMember moi = joueurs.stream().filter(m -> memberId.equals(m.getMemberId())).findFirst().orElse(null);
        String jour = session.startedAt().atZone(PARIS).format(JOUR);
        StringBuilder texte = new StringBuilder()
                .append("**").append(session.planned() ? "Séance d'entraînement du " : "Session du ").append(jour)
                .append("** · ").append(team.getName()).append('\n')
                .append(session.games()).append(" parties : ").append(session.wins()).append(" V – ")
                .append(session.games() - session.wins()).append(" D.\n");
        if (moi != null && moi.getRiotPuuid() != null) {
            List<String> toi = toi(moi.getRiotPuuid(), session);
            if (!toi.isEmpty()) {
                texte.append("\n**Toi**\n").append(String.join("\n", toi)).append('\n');
            }
        }
        List<String> equipe = equipe(team, session, joueurs, noms);
        if (!equipe.isEmpty()) {
            texte.append("\n**L'équipe**\n").append(String.join("\n", equipe)).append('\n');
        }
        texte.append("\nLe détail : ").append(premadelabUrl).append("/teams/").append(team.getId());
        return texte.toString();
    }

    private record Mesure(String libelle, ToDoubleFunction<RiotStatsGateway.Bucket> valeur, boolean plusHautMieux,
                          String format) {
    }

    private static final List<Mesure> MESURES = List.of(
            new Mesure("KDA", b -> (b.kills() + b.assists()) / (double) Math.max(1, b.deaths()), true, "%.1f"),
            new Mesure("CS/min", b -> b.minionsKilled() / minutes(b), true, "%.1f"),
            new Mesure("Or/min", b -> b.goldEarned() / minutes(b), true, "%.0f"),
            new Mesure("Dégâts/min", b -> b.damageToChampions() / minutes(b), true, "%.0f"),
            new Mesure("Vision/min", b -> b.visionScore() / minutes(b), true, "%.2f"),
            new Mesure("Morts par partie", b -> b.deaths() / (double) Math.max(1, b.games()), false, "%.1f"));

    private static double minutes(RiotStatsGateway.Bucket b) {
        return Math.max(1, b.secondsPlayed() / 60.0);
    }

    // Le joueur comparé à lui-même, jamais aux autres : deux points forts, deux à travailler.
    private List<String> toi(String puuid, TeamSession session) {
        Optional<RiotStatsGateway.Bucket> soir = statsGateway.aggregate(List.of(puuid),
                        RiotStatsGateway.Grouping.OVERALL, RiotStatsGateway.Scope.RIFT, null, session.matchIds())
                .flatMap(l -> l.stream().findFirst());
        Optional<RiotStatsGateway.Bucket> habitude = statsGateway.aggregate(List.of(puuid),
                        RiotStatsGateway.Grouping.OVERALL, RiotStatsGateway.Scope.RIFT,
                        Instant.now().minus(REFERENCE_JOURS, ChronoUnit.DAYS))
                .flatMap(l -> l.stream().findFirst());
        if (soir.isEmpty() || habitude.isEmpty() || soir.get().games() == 0) {
            return List.of();
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
        List<String> lignes = new ArrayList<>();
        ecarts.stream().filter(e -> e.relatif() >= ECART_NOTABLE)
                .sorted(Comparator.comparingDouble(Ecart::relatif).reversed()).limit(2)
                .forEach(e -> lignes.add("✅ " + ligne(e.mesure(), e.soir(), e.habitude())));
        ecarts.stream().filter(e -> e.relatif() <= -ECART_NOTABLE)
                .sorted(Comparator.comparingDouble(Ecart::relatif)).limit(2)
                .forEach(e -> lignes.add("🔧 " + ligne(e.mesure(), e.soir(), e.habitude())));
        return lignes;
    }

    private static String ligne(Mesure mesure, double soir, double habitude) {
        return mesure.libelle() + " : " + String.format(Locale.FRANCE, mesure.format(), soir)
                + " (ta moyenne : " + String.format(Locale.FRANCE, mesure.format(), habitude) + ")";
    }

    private List<String> equipe(Team team, TeamSession session, List<TeamMember> joueurs, Map<String, String> noms) {
        List<String> lignes = new ArrayList<>();
        Map<String, TeamMember> parPuuid = TeamPlayerStatsService.parPuuid(joueurs);
        List<RiotStatsGateway.SharedMatch> parties = statsGateway.sharedMatchesAmong(List.copyOf(parPuuid.keySet()),
                        SessionWatcher.MEMBRES_MINIMUM, session.matchIds())
                .map(RiotStatsGateway.SharedMatches::matches).orElseGet(List::of);

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
                    lignes.add("Duo du soir : " + noms.get(paire[0]) + " et " + noms.get(paire[1]) + ", "
                            + e.getValue()[1] + " V – " + (e.getValue()[0] - e.getValue()[1]) + " D.");
                });

        // Les défaites après une avance à 15 min, quand la timeline est relevée.
        long avancesPerdues = statsGateway.insights(session.matchIds()).orElseGet(List::of).stream()
                .filter(insight -> insight.participants() != null)
                .filter(insight -> parties.stream().anyMatch(p -> p.matchId().equals(insight.matchId())
                        && Boolean.FALSE.equals(p.win()) && avance(insight, p) >= AVANCE_A_15))
                .count();
        if (avancesPerdues > 0) {
            lignes.add(avancesPerdues + (avancesPerdues > 1 ? " parties perdues" : " partie perdue")
                    + " après une avance à 15 min.");
        }

        if (session.planned()) {
            sessions.findByTeamIdAndDayGreaterThanEqualOrderByStartedAtDesc(team.getId(),
                            session.day().minusDays(REFERENCE_JOURS)).stream()
                    .filter(s -> s.planned() && !s.missed() && s.startedAt().isBefore(session.startedAt()))
                    .findFirst()
                    .ifPresent(avant -> lignes.add("Par rapport à la séance précédente : "
                            + Math.round(100.0 * session.wins() / session.games()) + " % de victoires contre "
                            + Math.round(100.0 * avant.wins() / Math.max(1, avant.games())) + " %."));
        }
        return lignes;
    }

    private static int avance(RiotStatsGateway.Insight insight, RiotStatsGateway.SharedMatch partie) {
        int camp = partie.players().get(0).side();
        int nous = 0;
        int eux = 0;
        Set<String> vus = new HashSet<>();
        for (RiotStatsGateway.InsightPlayer joueur : insight.participants()) {
            if (joueur.at15() == null || !vus.add(joueur.puuid())) {
                continue;
            }
            if (joueur.side() == camp) {
                nous += joueur.at15().gold();
            } else {
                eux += joueur.at15().gold();
            }
        }
        return nous - eux;
    }
}
