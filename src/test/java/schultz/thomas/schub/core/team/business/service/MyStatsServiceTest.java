package schultz.thomas.schub.core.team.business.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import schultz.thomas.schub.core.business.service.RiotConnectorService;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.team.api.dto.MyStatsDto;
import schultz.thomas.schub.core.team.business.model.StatsState;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Mes stats : le sujet est l'acteur, et rien d'autre ne peut l'être. */
class MyStatsServiceTest {

    private static final String PUUID = "puuid-a-moi";
    private static final Instant QUAND = Instant.parse("2026-09-20T18:00:00Z");

    private RiotStatsGateway statsGateway;
    private RiotConnectorService riotConnector;
    private MyStatsService service;

    @BeforeEach
    void setUp() {
        statsGateway = mock(RiotStatsGateway.class);
        RiotChampionGateway championGateway = mock(RiotChampionGateway.class);
        riotConnector = mock(RiotConnectorService.class);
        service = new MyStatsService(
                new PlayerStatsService(statsGateway, championGateway, riotConnector),
                riotConnector);

        when(championGateway.catalogue()).thenReturn(Optional.empty());
        when(riotConnector.ingestOf(anyString())).thenReturn(Optional.empty());
        when(statsGateway.aggregate(any(), any(), any())).thenReturn(Optional.of(List.of()));
        when(statsGateway.rankings(anyString())).thenReturn(Optional.of(List.of()));
        when(statsGateway.coverage(any())).thenReturn(Optional.of(List.of(
                new RiotStatsGateway.Coverage(PUUID, true, 0, 0, null, null, null))));
    }

    @Test
    @DisplayName("Sans compte Riot lié : pas d'erreur, un état qui dit pourquoi, aucun appel")
    void sansCompteRiot() {
        MyStatsDto stats = service.of(compte(null), null, null);

        assertThat(stats.state()).isEqualTo(StatsState.COMPTE_RIOT_ABSENT);
        assertThat(stats.overall()).isNull();
        assertThat(stats.champions()).isEmpty();
        verify(statsGateway, never()).aggregate(any(), any(), any());
        verify(riotConnector, never()).ingestOf(anyString());
    }

    @Test
    @DisplayName("Collecte en cours : l'échéance est portée par la réponse, pas devinée par l'écran")
    void ingestionEnCours() {
        when(riotConnector.ingestOf(PUUID)).thenReturn(Optional.of(
                new RiotConnectorService.PlayerIngest(430, 1, QUAND)));

        MyStatsDto stats = service.of(compte(PUUID), null, null);

        assertThat(stats.state()).isEqualTo(StatsState.INGESTION_EN_COURS);
        assertThat(stats.ingest().pending()).isEqualTo(430);
        assertThat(stats.ingest().estimatedReadyAt()).isEqualTo(QUAND);
    }

    @Test
    @DisplayName("La fenêtre demandée est celle qui part au connecteur, et elle est redite dans la réponse")
    void fenetreDemandee() {
        MyStatsDto stats = service.of(compte(PUUID), 30, null);

        assertThat(stats.days()).isEqualTo(30);
        verify(statsGateway).aggregate(eq(List.of(PUUID)),
                eq(RiotStatsGateway.Grouping.OVERALL), any(Instant.class));
    }

    private static User compte(String puuid) {
        User user = new User();
        user.setId("user-1");
        user.setDiscordId("discord-1");
        user.setDisplayName("Moi");
        user.setRiotPuuid(puuid);
        user.setRiotGameName("Moi");
        user.setRiotTagLine("EUW");
        return user;
    }
}
