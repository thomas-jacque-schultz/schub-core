package schultz.thomas.schub.core.lol.queuekiller.business.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import schultz.thomas.schub.core.business.service.UserService;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.lol.queuekiller.business.model.PremadeRoom;
import schultz.thomas.schub.core.lol.team.business.service.TeamService;
import schultz.thomas.schub.core.lol.team.data.model.Team;
import schultz.thomas.schub.core.lol.team.data.model.TeamMember;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

// Qui joue où, parmi les coéquipiers (Schub#149). En mémoire seulement : la page du salon redit sa présence toutes
// les 30 s, et un redémarrage du cœur ne perd qu'une minute de présences.
@Service
public class QueueKillerPresenceService {

    static final Duration EXPIRATION = Duration.ofSeconds(90);
    private static final Pattern JEU = Pattern.compile("[a-z]{2,20}+");
    private static final Pattern SALON = Pattern.compile("[A-Z]{4}+");

    private record Presence(String game, String room, Instant seenAt) {

        boolean memeSalon(String autreJeu, String autreSalon) {
            return game.equals(autreJeu) && room.equals(autreSalon);
        }
    }

    private final TeamService teams;
    private final UserService users;
    private final Clock clock;
    private final Map<String, Presence> presences = new ConcurrentHashMap<>();

    @Autowired
    public QueueKillerPresenceService(TeamService teams, UserService users) {
        this(teams, users, Clock.systemUTC());
    }

    QueueKillerPresenceService(TeamService teams, UserService users, Clock clock) {
        this.teams = teams;
        this.users = users;
        this.clock = clock;
    }

    public void signal(User actor, String game, String room) {
        if (game == null || room == null || !JEU.matcher(game).matches() || !SALON.matcher(room).matches()) {
            throw new IllegalArgumentException("Jeu ou salon invalide.");
        }
        presences.put(actor.getId(), new Presence(game, room, clock.instant()));
    }

    public void leave(User actor, String game, String room) {
        presences.computeIfPresent(actor.getId(),
                (id, presence) -> presence.memeSalon(game, room) ? null : presence);
    }

    public List<PremadeRoom> premadeRooms(User actor) {
        Instant limite = clock.instant().minus(EXPIRATION);
        presences.values().removeIf(presence -> presence.seenAt().isBefore(limite));

        Set<String> coequipiers = new HashSet<>();
        for (Team team : teams.mine(actor)) {
            coequipiers.add(team.getCreatedBy());
            team.getMembers().stream().filter(TeamMember::isLinked).map(TeamMember::getUserId)
                    .forEach(coequipiers::add);
        }
        coequipiers.remove(actor.getId());
        coequipiers.remove(null);

        Presence mienne = presences.get(actor.getId());
        Map<String, List<String>> parSalon = new LinkedHashMap<>();
        Map<String, Presence> salons = new LinkedHashMap<>();
        for (String userId : coequipiers) {
            Presence presence = presences.get(userId);
            if (presence == null || mienne != null && mienne.memeSalon(presence.game(), presence.room())) {
                continue;
            }
            String cle = presence.game() + "/" + presence.room();
            salons.putIfAbsent(cle, presence);
            parSalon.computeIfAbsent(cle, k -> new ArrayList<>()).add(userId);
        }
        if (parSalon.isEmpty()) {
            return List.of();
        }

        Map<String, User> comptes = users.byIds(new HashSet<>(parSalon.values().stream().flatMap(List::stream).toList()));
        return parSalon.entrySet().stream()
                .map(entree -> new PremadeRoom(
                        salons.get(entree.getKey()).game(),
                        salons.get(entree.getKey()).room(),
                        entree.getValue().stream()
                                .map(comptes::get)
                                .filter(Objects::nonNull)
                                .map(user -> new PremadeRoom.Player(users.displayNameOf(user), user.getAvatarUrl()))
                                .sorted(Comparator.comparing(PremadeRoom.Player::displayName,
                                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                                .toList()))
                .filter(salon -> !salon.players().isEmpty())
                .sorted(Comparator.comparingInt((PremadeRoom salon) -> salon.players().size()).reversed()
                        .thenComparing(PremadeRoom::game).thenComparing(PremadeRoom::room))
                .toList();
    }
}
