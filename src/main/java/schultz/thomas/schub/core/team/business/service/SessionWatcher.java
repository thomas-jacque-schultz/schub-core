package schultz.thomas.schub.core.team.business.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import schultz.thomas.schub.core.business.service.RiotConnectorService;
import schultz.thomas.schub.core.team.data.model.Team;
import schultz.thomas.schub.core.team.data.model.TeamMember;
import schultz.thomas.schub.core.team.data.model.TeamSession;
import schultz.thomas.schub.core.team.data.model.TrainingSlot;
import schultz.thomas.schub.core.team.data.repository.TeamRepository;
import schultz.thomas.schub.core.team.data.repository.TeamSessionRepository;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

// L'observateur de sessions (Schub#103). La journée de jeu va de 6 h à 6 h, heure de Paris : une soirée qui
// déborde après minuit reste celle de la veille.
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionWatcher {

    static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    // Réponse du 05/10 : à deux, c'est juste normal ; à trois, c'est un début d'équipe.
    static final int MEMBRES_MINIMUM = 3;
    static final int PARTIES_MINIMUM = 2;
    static final Duration PAUSE_MAX = Duration.ofHours(1);
    static final int DEBUT_DE_JOURNEE = 6;

    private final TeamRepository teams;
    private final TeamSessionRepository sessions;
    private final RiotStatsGateway statsGateway;
    private final RiotConnectorService riotConnector;

    // 4 h : les parties de la veille sont relevées avant la détection de 6 h. Un coût de quota par membre et par jour.
    @Scheduled(cron = "0 0 4 * * *", zone = "Europe/Paris")
    public void prepare() {
        teams.findAll().forEach(team -> TeamPlayerStatsService.joueursDe(team).stream()
                .map(TeamMember::getRiotPuuid).filter(Objects::nonNull).distinct()
                .forEach(riotConnector::requestIngest));
    }

    @Scheduled(cron = "0 0 6 * * *", zone = "Europe/Paris")
    public void detectYesterday() {
        LocalDate hier = LocalDate.now(PARIS).minusDays(1);
        teams.findAll().forEach(team -> {
            try {
                detect(team, hier);
            } catch (RuntimeException e) {
                log.warn("Sessions du {} non détectées pour l'équipe {} : {}", hier, team.getId(), e.getMessage());
            }
        });
    }

    public List<TeamSession> detect(Team team, LocalDate jour) {
        Map<String, TeamMember> parPuuid = TeamPlayerStatsService.parPuuid(TeamPlayerStatsService.joueursDe(team));
        Instant debut = jour.atTime(DEBUT_DE_JOURNEE, 0).atZone(PARIS).toInstant();
        Instant fin = debut.plus(Duration.ofDays(1));
        List<RiotStatsGateway.SharedMatch> parties = parPuuid.size() < MEMBRES_MINIMUM ? List.of()
                : statsGateway.sharedMatches(List.copyOf(parPuuid.keySet()), MEMBRES_MINIMUM, debut,
                                TeamGamesStatsService.PARTIES_MAX)
                        .map(RiotStatsGateway.SharedMatches::matches).orElseGet(List::of).stream()
                        .filter(p -> p.startedAt() != null && p.startedAt().isBefore(fin) && !p.splitSides())
                        .toList();
        List<TeamSession> trouvees = new ArrayList<>();
        for (List<RiotStatsGateway.SharedMatch> groupe : groupes(parties)) {
            Instant ouverture = groupe.get(0).startedAt();
            RiotStatsGateway.SharedMatch derniere = groupe.get(groupe.size() - 1);
            Instant fermeture = derniere.startedAt().plusSeconds(derniere.durationSeconds());
            TrainingSlot creneau = creneau(team, jour, ouverture, fermeture);
            Set<String> membres = new TreeSet<>();
            groupe.forEach(p -> p.players().forEach(j -> {
                TeamMember m = parPuuid.get(j.puuid());
                if (m != null) {
                    membres.add(m.getMemberId());
                }
            }));
            trouvees.add(new TeamSession(team.getId() + "|" + ouverture, team.getId(), jour, ouverture, fermeture,
                    groupe.stream().map(RiotStatsGateway.SharedMatch::matchId).toList(), List.copyOf(membres),
                    groupe.size(), groupe.stream().filter(p -> Boolean.TRUE.equals(p.win())).count(),
                    creneau != null, creneau == null ? null : creneau.id(), false, Instant.now()));
        }
        for (TrainingSlot slot : creneauxDuJour(team, jour)) {
            boolean joue = trouvees.stream().anyMatch(s -> slot.id().equals(s.slotId()));
            if (!joue) {
                Instant[] bornes = bornes(slot, jour);
                trouvees.add(new TeamSession(team.getId() + "|" + bornes[0] + "|manquee", team.getId(), jour,
                        bornes[0], bornes[1], List.of(), List.of(), 0, 0, true, slot.id(), true, Instant.now()));
            }
        }
        sessions.saveAll(trouvees);
        return trouvees;
    }

    // Des parties à au moins trois membres, à la suite, sans pause de plus d'une heure ; deux au moins.
    static List<List<RiotStatsGateway.SharedMatch>> groupes(List<RiotStatsGateway.SharedMatch> parties) {
        List<RiotStatsGateway.SharedMatch> triees = parties.stream()
                .sorted(Comparator.comparing(RiotStatsGateway.SharedMatch::startedAt)).toList();
        List<List<RiotStatsGateway.SharedMatch>> groupes = new ArrayList<>();
        List<RiotStatsGateway.SharedMatch> courant = new ArrayList<>();
        for (RiotStatsGateway.SharedMatch partie : triees) {
            if (!courant.isEmpty()) {
                RiotStatsGateway.SharedMatch avant = courant.get(courant.size() - 1);
                Instant finAvant = avant.startedAt().plusSeconds(avant.durationSeconds());
                if (Duration.between(finAvant, partie.startedAt()).compareTo(PAUSE_MAX) > 0) {
                    groupes.add(courant);
                    courant = new ArrayList<>();
                }
            }
            courant.add(partie);
        }
        if (!courant.isEmpty()) {
            groupes.add(courant);
        }
        return groupes.stream().filter(g -> g.size() >= PARTIES_MINIMUM).toList();
    }

    static List<TrainingSlot> creneauxDuJour(Team team, LocalDate jour) {
        if (team.getTrainingSlots() == null) {
            return List.of();
        }
        return team.getTrainingSlots().stream()
                .filter(s -> s.recurring() ? s.day() == jour.getDayOfWeek() : jour.equals(s.localDate()))
                .toList();
    }

    static Instant[] bornes(TrainingSlot slot, LocalDate jour) {
        ZonedDateTime debut = jour.atTime(slot.startTime()).atZone(PARIS);
        ZonedDateTime fin = jour.atTime(slot.endTime()).atZone(PARIS);
        if (!fin.isAfter(debut)) {
            fin = fin.plusDays(1);
        }
        return new Instant[]{debut.toInstant(), fin.toInstant()};
    }

    private static TrainingSlot creneau(Team team, LocalDate jour, Instant ouverture, Instant fermeture) {
        return creneauxDuJour(team, jour).stream()
                .filter(slot -> {
                    Instant[] b = bornes(slot, jour);
                    return ouverture.isBefore(b[1]) && fermeture.isAfter(b[0]);
                })
                .findFirst().orElse(null);
    }
}
