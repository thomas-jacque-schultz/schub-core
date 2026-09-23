package schultz.thomas.schub.core.team.business.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import schultz.thomas.schub.core.business.service.RiotConnectorService;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.team.api.dto.StatsRefreshDto;
import schultz.thomas.schub.core.team.data.model.Team;
import schultz.thomas.schub.core.team.data.model.TeamMember;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TeamStatsRefreshServiceTest {

    private final TeamService teamService = mock(TeamService.class);
    private final RiotConnectorService riot = mock(RiotConnectorService.class);
    private final MongoTemplate mongo = mock(MongoTemplate.class);
    private final TeamStatsRefreshService service = new TeamStatsRefreshService(teamService, riot, mongo);
    private final User lecteur = new User();
    private final Team equipe = new Team();

    @BeforeEach
    void setUp() {
        equipe.setId("equipe-1");
        equipe.setName("Les cinq");
        equipe.getMembers().add(membre("p1"));
        equipe.getMembers().add(membre("p2"));
        equipe.getMembers().add(membre("p1"));
        equipe.getMembers().add(membre(null));
        when(teamService.requireVisible(lecteur, "equipe-1")).thenReturn(equipe);
        when(riot.requestIngest(anyString())).thenReturn(true);
    }

    @Test
    @DisplayName("hors délai : une collecte par joueur lié, et la prochaine dans trois minutes")
    void declenche() {
        when(mongo.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class),
                eq(Team.class))).thenReturn(equipe);

        StatsRefreshDto reponse = service.refresh(lecteur, "equipe-1");

        assertThat(reponse.triggered()).isTrue();
        assertThat(reponse.playersQueued()).isEqualTo(2);
        assertThat(reponse.nextAllowedAt()).isAfter(Instant.now().plusSeconds(170));
        verify(riot, times(1)).requestIngest("p1");
        verify(riot, times(1)).requestIngest("p2");
    }

    @Test
    @DisplayName("dans le délai : rien n'est demandé à Riot, et l'heure de la prochaine est rendue")
    void refuseDansLeDelai() {
        equipe.setStatsRefreshedAt(Instant.now().minusSeconds(60));
        when(mongo.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class),
                eq(Team.class))).thenReturn(null);

        StatsRefreshDto reponse = service.refresh(lecteur, "equipe-1");

        assertThat(reponse.triggered()).isFalse();
        assertThat(reponse.nextAllowedAt()).isAfter(Instant.now().plusSeconds(100));
        verify(riot, never()).requestIngest(anyString());
    }

    private static TeamMember membre(String puuid) {
        TeamMember member = new TeamMember();
        member.setRiotPuuid(puuid);
        return member;
    }
}
