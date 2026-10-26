package schultz.thomas.schub.core.lol.team.business.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import schultz.thomas.schub.core.business.service.RiotAccountService;
import schultz.thomas.schub.core.business.service.RiotConnectorService;
import schultz.thomas.schub.core.business.service.RiotIdResolution;
import schultz.thomas.schub.core.business.service.RiotIdResolver;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.data.repository.UserRepository;
import schultz.thomas.schub.core.lol.team.data.model.Team;
import schultz.thomas.schub.core.lol.team.data.model.TeamMember;
import schultz.thomas.schub.core.lol.team.data.repository.TeamRepository;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StalePuuidRepairTest {

    private final RiotConnectorService connector = mock(RiotConnectorService.class);
    private final RiotAccountService riotAccounts = mock(RiotAccountService.class);
    private final RiotIdResolver resolver = mock(RiotIdResolver.class);
    private final UserRepository users = mock(UserRepository.class);
    private final TeamRepository teams = mock(TeamRepository.class);
    private final StalePuuidRepair repair = new StalePuuidRepair(connector, riotAccounts, resolver, users, teams);

    @Test
    @DisplayName("Une place d'équipe au puuid périmé prend le puuid de la nouvelle résolution, puis sa collecte repart")
    void placeReparee() {
        Team team = equipe(membre("m1", "ancien", "Joueur", "EUW"), membre("m2", "sain", "Autre", "EUW"));
        when(users.findByRiotPuuidIn(any())).thenReturn(List.of());
        when(teams.findByMembersRiotPuuidIn(any())).thenReturn(List.of(team));
        when(resolver.resolve("Joueur", "EUW")).thenReturn(RiotIdResolution.resolved("nouveau"));

        repair.repare(List.of("ancien"));

        assertThat(team.getMembers()).extracting(TeamMember::getRiotPuuid).containsExactly("nouveau", "sain");
        verify(teams).save(team);
        verify(connector).requestIngest("nouveau");
    }

    @Test
    @DisplayName("Une place ajoutée sans puuid, connecteur occupé, est résolue au relevé suivant sous le nom actuel du joueur")
    void placeEnAttenteResolue() {
        Team team = equipe(membre("m1", null, "MimiQueue", "PIKA"), membre("m2", "sain", "Autre", "EUW"));
        when(teams.findWithUnresolvedMembers()).thenReturn(List.of(team));
        when(resolver.resolve("MimiQueue", "PIKA")).thenReturn(RiotIdResolution.resolved("p1", "Memero", "MIAM"));

        repair.resoutLesPlacesEnAttente();

        TeamMember place = team.getMembers().get(0);
        assertThat(place.getRiotPuuid()).isEqualTo("p1");
        assertThat(place.riotId()).isEqualTo("Memero#MIAM");
        verify(teams).save(team);
        verify(connector).requestIngest("p1");
        verify(resolver, never()).resolve("Autre", "EUW");
    }

    @Test
    @DisplayName("Une place résolue après coup est reliée au compte qui porte son puuid (Schub#157)")
    void placeResolueReliee() {
        Team team = equipe(membre("m1", "p1", "Sbrawa", "EUW"), membre("m2", "p2", "Autre", "EUW"));
        when(teams.findWithUnlinkedResolvedMembers()).thenReturn(List.of(team));
        when(users.findByRiotPuuidIn(any())).thenReturn(List.of(compte("u1", "p1")));

        repair.relieLesPlacesLibres();

        assertThat(team.getMembers()).extracting(TeamMember::getUserId).containsExactly("u1", null);
        assertThat(team.getMembers().get(0).getLinkedAt()).isNotNull();
        verify(teams).save(team);
    }

    @Test
    @DisplayName("Un compte qui a déjà sa place dans l'équipe n'en prend pas une deuxième")
    void compteDejaDansLEquipe() {
        TeamMember dejaLie = membre("m1", "p1", "Sbrawa", "EUW");
        dejaLie.setUserId("u1");
        Team team = equipe(dejaLie, membre("m2", "p1", "Sbrawa", "EUW"));
        when(teams.findWithUnlinkedResolvedMembers()).thenReturn(List.of(team));
        when(users.findByRiotPuuidIn(any())).thenReturn(List.of(compte("u1", "p1")));

        repair.relieLesPlacesLibres();

        assertThat(team.getMembers().get(1).isLinked()).isFalse();
        verify(teams, never()).save(any());
    }

    @Test
    @DisplayName("Un compte en attente de résolution est retenté au relevé, sans attendre sa connexion (schub-core#47)")
    void compteEnAttenteRetente() {
        User user = new User();
        user.setId("u1");
        user.setRiotGameName("Joueur");
        user.setRiotTagLine("EUW");
        when(users.findByRiotPuuidIsNullAndRiotTagLineIsNotNull()).thenReturn(List.of(user));

        repair.resoutLesComptesEnAttente();

        verify(riotAccounts).resolvePendingLink(user);
    }

    @Test
    @DisplayName("Sans résolution, la place garde son puuid et l'équipe n'est pas réécrite")
    void resolutionReportee() {
        Team team = equipe(membre("m1", "ancien", "Joueur", "EUW"));
        when(users.findByRiotPuuidIn(any())).thenReturn(List.of());
        when(teams.findByMembersRiotPuuidIn(any())).thenReturn(List.of(team));
        when(resolver.resolve("Joueur", "EUW")).thenReturn(RiotIdResolution.busy());

        repair.repare(List.of("ancien"));

        verify(teams, never()).save(any());
    }

    @Test
    @DisplayName("Un compte lié au puuid périmé est vidé puis résolu de nouveau")
    void compteLie() {
        User user = new User();
        user.setId("u1");
        user.setRiotPuuid("ancien");
        when(users.findByRiotPuuidIn(any())).thenReturn(List.of(user));
        when(users.save(user)).thenReturn(user);
        when(teams.findByMembersRiotPuuidIn(any())).thenReturn(List.of());

        repair.repare(List.of("ancien"));

        assertThat(user.getRiotPuuid()).isNull();
        verify(riotAccounts).resolvePendingLink(user);
    }

    private static Team equipe(TeamMember... membres) {
        Team team = new Team();
        team.setId("t1");
        team.setName("Miam");
        team.setMembers(new ArrayList<>(List.of(membres)));
        return team;
    }

    private static User compte(String id, String puuid) {
        User user = new User();
        user.setId(id);
        user.setRiotPuuid(puuid);
        return user;
    }

    private static TeamMember membre(String id, String puuid, String nom, String tag) {
        TeamMember membre = new TeamMember();
        membre.setMemberId(id);
        membre.setRiotPuuid(puuid);
        membre.setRiotGameName(nom);
        membre.setRiotTagLine(tag);
        return membre;
    }
}
