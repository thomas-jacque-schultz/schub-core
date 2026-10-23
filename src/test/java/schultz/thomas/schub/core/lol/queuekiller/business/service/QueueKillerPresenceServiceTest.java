package schultz.thomas.schub.core.lol.queuekiller.business.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import schultz.thomas.schub.core.business.service.UserService;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.lol.queuekiller.business.model.PremadeRoom;
import schultz.thomas.schub.core.lol.team.business.service.TeamService;
import schultz.thomas.schub.core.lol.team.data.model.Team;
import schultz.thomas.schub.core.lol.team.data.model.TeamMember;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QueueKillerPresenceServiceTest {

    private final TeamService teams = mock(TeamService.class);
    private final UserService users = mock(UserService.class);
    private final Clock clock = mock(Clock.class);
    private final QueueKillerPresenceService service = new QueueKillerPresenceService(teams, users, clock);
    private final Map<String, User> comptes = new HashMap<>();
    private Instant maintenant = Instant.parse("2026-10-23T18:00:00Z");

    private final User moi = compte("moi", "Moi");
    private final User leo = compte("leo", "Léo");
    private final User max = compte("max", "Max");
    private final User inconnu = compte("inconnu", "Inconnu");

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenAnswer(appel -> maintenant);
        when(users.byIds(anySet())).thenAnswer(appel -> {
            Set<String> ids = appel.getArgument(0);
            Map<String, User> trouves = new HashMap<>();
            ids.forEach(id -> trouves.put(id, comptes.get(id)));
            return trouves;
        });
        when(users.displayNameOf(any())).thenAnswer(appel -> ((User) appel.getArgument(0)).getDisplayName());
        when(teams.mine(moi)).thenReturn(List.of(equipe("moi", "leo", "max", null)));
    }

    @Test
    @DisplayName("Je vois le salon de mes coéquipiers, pas celui d'un inconnu ni le mien")
    void coequipiersSeulement() {
        service.signal(leo, "quiz", "KQXP");
        service.signal(max, "quiz", "KQXP");
        service.signal(inconnu, "zoom", "ABCD");
        service.signal(moi, "scribble", "MOIS");

        List<PremadeRoom> salons = service.premadeRooms(moi);

        assertThat(salons).singleElement().satisfies(salon -> {
            assertThat(salon.game()).isEqualTo("quiz");
            assertThat(salon.room()).isEqualTo("KQXP");
            assertThat(salon.players()).extracting(PremadeRoom.Player::displayName).containsExactly("Léo", "Max");
        });
    }

    @Test
    @DisplayName("Un salon où je suis déjà ne m'est pas proposé")
    void pasMonPropreSalon() {
        service.signal(leo, "quiz", "KQXP");
        service.signal(moi, "quiz", "KQXP");

        assertThat(service.premadeRooms(moi)).isEmpty();
    }

    @Test
    @DisplayName("Une présence expire sans signal, et disparaît aussitôt quand on quitte le salon")
    void expirationEtDepart() {
        service.signal(leo, "quiz", "KQXP");
        service.signal(max, "zoom", "ABCD");
        maintenant = maintenant.plus(QueueKillerPresenceService.EXPIRATION).plusSeconds(1);
        service.signal(max, "zoom", "ABCD");

        assertThat(service.premadeRooms(moi)).extracting(PremadeRoom::room).containsExactly("ABCD");

        service.leave(max, "zoom", "WXYZ");
        assertThat(service.premadeRooms(moi)).hasSize(1);
        service.leave(max, "zoom", "ABCD");
        assertThat(service.premadeRooms(moi)).isEmpty();
    }

    @Test
    @DisplayName("Un jeu ou un code de salon mal formé est refusé")
    void validation() {
        assertThatThrownBy(() -> service.signal(leo, "quiz", "abcd")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.signal(leo, "Quiz!", "ABCD")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.signal(leo, null, "ABCD")).isInstanceOf(IllegalArgumentException.class);
    }

    private User compte(String id, String nom) {
        User user = new User();
        user.setId(id);
        user.setDisplayName(nom);
        comptes.put(id, user);
        return user;
    }

    private static Team equipe(String createur, String... membres) {
        Team team = new Team();
        team.setCreatedBy(createur);
        for (String userId : membres) {
            TeamMember membre = new TeamMember();
            membre.setUserId(userId);
            team.getMembers().add(membre);
        }
        return team;
    }
}
