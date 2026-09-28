package schultz.thomas.schub.core.team.business.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import schultz.thomas.schub.core.business.service.RiotAccountService;
import schultz.thomas.schub.core.business.service.RiotConnectorService;
import schultz.thomas.schub.core.business.service.RiotIdResolution;
import schultz.thomas.schub.core.business.service.RiotIdResolver;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.data.repository.UserRepository;
import schultz.thomas.schub.core.team.data.model.Team;
import schultz.thomas.schub.core.team.data.model.TeamMember;
import schultz.thomas.schub.core.team.data.repository.TeamRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// Riot chiffre le puuid pour chaque clé : après un changement de clé, les comptes liés et les places d'équipe portent un
// puuid refusé. On résout de nouveau leur Riot ID et on relance leur collecte.
@Slf4j
@Service
@RequiredArgsConstructor
public class StalePuuidRepair {

    private static final Duration RECOUVREMENT = Duration.ofMinutes(10);

    private final RiotConnectorService riotConnector;
    private final RiotAccountService riotAccounts;
    private final RiotIdResolver resolver;
    private final UserRepository users;
    private final TeamRepository teams;

    private volatile Instant dernierReleve = Instant.now().minus(Duration.ofDays(1));

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void verifieAuDemarrage() {
        Set<String> puuids = puuidsSuivis();
        List<String> perimes = riotConnector.checkPuuids(puuids);
        log.info("Puuid des comptes liés et des places d'équipe soumis au connecteur : {}, dont {} déjà refusés",
                puuids.size(), perimes.size());
        repare(perimes);
    }

    // Après l'invalidation des données Riot : chaque puuid suivi est tenu pour périmé, qu'il le soit ou non.
    @Async
    public void resoutTout(Collection<String> puuids) {
        repare(puuids);
        log.info("Invalidation des données Riot : {} puuid repassés par la résolution de leur Riot ID", puuids.size());
    }

    public Set<String> puuidsSuivis() {
        Set<String> puuids = new LinkedHashSet<>();
        users.findAll().stream().map(User::getRiotPuuid).filter(StalePuuidRepair::present).forEach(puuids::add);
        teams.findRiotPuuids().forEach(team -> team.getMembers().stream()
                .map(TeamMember::getRiotPuuid).filter(StalePuuidRepair::present).forEach(puuids::add));
        return puuids;
    }

    @Scheduled(fixedDelayString = "PT5M", initialDelayString = "PT5M")
    public void releve() {
        Instant debut = Instant.now();
        repare(riotConnector.stalePuuids(dernierReleve.minus(RECOUVREMENT)));
        resoutLesPlacesEnAttente();
        dernierReleve = debut;
    }

    // Une place ajoutée pendant que le connecteur était occupé n'a pas de puuid : rien d'autre ne la retenterait.
    void resoutLesPlacesEnAttente() {
        for (Team team : teams.findWithUnresolvedMembers()) {
            Set<String> aCollecter = new LinkedHashSet<>();
            for (TeamMember membre : team.getMembers()) {
                if (!present(membre.getRiotPuuid()) && resout(team, membre)) {
                    aCollecter.add(membre.getRiotPuuid());
                }
            }
            enregistre(team, aCollecter, "place(s) en attente résolue(s)");
        }
    }

    void repare(Collection<String> perimes) {
        if (perimes.isEmpty()) {
            return;
        }
        Set<String> refuses = Set.copyOf(perimes);
        for (User user : users.findByRiotPuuidIn(refuses)) {
            user.setRiotPuuid(null);
            boolean resolu = riotAccounts.resolvePendingLink(users.save(user));
            log.info("Compte lié {} au puuid périmé : {}", user.getId(),
                    resolu ? "Riot ID résolu de nouveau" : "résolution reportée à sa prochaine connexion");
        }
        for (Team team : teams.findByMembersRiotPuuidIn(refuses)) {
            repare(team, refuses);
        }
    }

    private void repare(Team team, Set<String> refuses) {
        Set<String> aCollecter = new LinkedHashSet<>();
        for (TeamMember membre : team.getMembers()) {
            if (refuses.contains(membre.getRiotPuuid()) && resout(team, membre)) {
                aCollecter.add(membre.getRiotPuuid());
            }
        }
        enregistre(team, aCollecter, "place(s) au puuid périmé réparée(s)");
    }

    // Le nom rendu par Riot remplace celui de la place : sa casse, ou le nom actuel d'un joueur renommé depuis.
    private boolean resout(Team team, TeamMember membre) {
        RiotIdResolution resolution = resolver.resolve(membre.getRiotGameName(), membre.getRiotTagLine());
        if (resolution.puuid() == null) {
            log.info("Place {} de l'équipe « {} » : Riot ID pas encore résolu ({})", membre.getMemberId(),
                    team.getName(), resolution.outcome());
            return false;
        }
        membre.setRiotPuuid(resolution.puuid());
        if (resolution.gameName() != null && resolution.tagLine() != null) {
            membre.setRiotGameName(resolution.gameName());
            membre.setRiotTagLine(resolution.tagLine());
        }
        return true;
    }

    private void enregistre(Team team, Set<String> aCollecter, String quoi) {
        if (aCollecter.isEmpty()) {
            return;
        }
        try {
            teams.save(team);
            aCollecter.forEach(riotConnector::requestIngest);
            log.info("Équipe « {} » : {} {}", team.getName(), aCollecter.size(), quoi);
        } catch (OptimisticLockingFailureException concurrente) {
            log.info("Équipe « {} » modifiée pendant la réparation : reprise au prochain relevé", team.getName());
        }
    }

    private static boolean present(String puuid) {
        return puuid != null && !puuid.isBlank();
    }
}
