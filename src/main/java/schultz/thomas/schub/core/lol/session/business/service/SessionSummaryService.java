package schultz.thomas.schub.core.lol.session.business.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import schultz.thomas.schub.core.business.service.PremadeLabGateway.Embed;
import schultz.thomas.schub.core.business.service.PremadeLabGateway;
import schultz.thomas.schub.core.data.model.SessionSummaryPreferences;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.data.repository.UserRepository;
import schultz.thomas.schub.core.lol.session.data.model.TeamSession;
import schultz.thomas.schub.core.lol.session.data.repository.TeamSessionRepository;
import schultz.thomas.schub.core.lol.team.data.model.Team;
import schultz.thomas.schub.core.lol.team.data.repository.TeamRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

// Le résumé de session en MP, le lendemain (Schub#99). Un MP par personne et par jour, aux seuls présents ;
// planifiées activées par défaut, libres désactivées (réponse du 05/10).
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionSummaryService {

    static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    // Limites de Discord : 10 encadrés et 6 000 caractères par message, 25 champs par encadré.
    static final int ENCADRES_MAX = 10;
    static final int CARACTERES_MAX = 6000;

    private final TeamSessionRepository sessions;
    private final TeamRepository teams;
    private final UserRepository users;
    private final PremadeLabGateway premadelab;
    private final SessionRecapComposer composer;

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
                    .map(s -> composer.compose(equipes.computeIfAbsent(s.teamId(), id -> teams.findById(id).orElse(null)),
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
                        .map(team -> composer.compose(team, s, cle.split("\\|")[1]))
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
}
