package schultz.thomas.schub.core.team.business.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import schultz.thomas.schub.core.business.service.RiotConnectorService;
import schultz.thomas.schub.core.team.api.dto.StatLineDto;
import schultz.thomas.schub.core.team.business.model.StatsState;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlayerStatsServiceTest {

    private static final String PUUID = "puuid-1";
    private static final Instant QUAND = Instant.parse("2026-09-20T18:00:00Z");

    private RiotStatsGateway statsGateway;
    private RiotChampionGateway championGateway;
    private RiotConnectorService riotConnector;
    private PlayerStatsService service;

    @BeforeEach
    void setUp() {
        statsGateway = mock(RiotStatsGateway.class);
        championGateway = mock(RiotChampionGateway.class);
        riotConnector = mock(RiotConnectorService.class);
        service = new PlayerStatsService(statsGateway, championGateway, riotConnector);

        when(championGateway.catalogue()).thenReturn(Optional.empty());
        when(statsGateway.coverage(any())).thenReturn(Optional.of(List.of(
                new RiotStatsGateway.Coverage(PUUID, true, 0, 0, null, null, QUAND))));
        when(statsGateway.aggregate(any(), any(), any(), any())).thenReturn(Optional.of(List.of()));
        when(riotConnector.ingestOf(anyString())).thenReturn(Optional.empty());
    }

    private void total(long games, long wins, long kills, long deaths, long assists) {
        when(statsGateway.aggregate(any(), eq(RiotStatsGateway.Grouping.OVERALL), any(), any()))
                .thenReturn(Optional.of(List.of(bucket("", games, wins, kills, deaths, assists))));
    }

    private static RiotStatsGateway.Bucket bucket(String key, long games, long wins, long kills,
                                                  long deaths, long assists) {
        return new RiotStatsGateway.Bucket(PUUID, key, "Jayce", games, wins, kills, deaths,
                assists, 0, 0, 0, 0, 0, 0, 0, 0, games * 1800, QUAND, QUAND, null);
    }

    private PlayerStatsService.Figures figures() {
        Map<String, PlayerStatsService.Figures> figures =
                service.of(List.of(PUUID), null, PlayerStatsService.CHAMPIONS_DEFAUT);
        return figures.get(PUUID);
    }

    @Test
    @DisplayName("Connecteur muet : on ne sait pas, et on ne dit pas « aucune partie »")
    void connecteurMuet() {
        when(statsGateway.aggregate(any(), eq(RiotStatsGateway.Grouping.OVERALL), any(), any()))
                .thenReturn(Optional.empty());

        assertThat(figures().state()).isEqualTo(StatsState.CONNECTEUR_INDISPONIBLE);
    }

    @Test
    @DisplayName("Aucune partie collectée et une collecte en file : l'écran attend, il n'est pas vide")
    void ingestionEnCours() {
        when(riotConnector.ingestOf(PUUID)).thenReturn(Optional.of(
                new RiotConnectorService.PlayerIngest(430, 1, QUAND)));

        assertThat(figures().state()).isEqualTo(StatsState.INGESTION_EN_COURS);
    }

    @Test
    @DisplayName("Rien en file, rien de relevé : aucune partie, et c'est une réponse")
    void aucunePartie() {
        assertThat(figures().state()).isEqualTo(StatsState.AUCUNE_PARTIE);
    }

    @Test
    @DisplayName("Des ids relevés sans détail : la collecte n'est pas finie")
    void detailsEnAttente() {
        when(statsGateway.coverage(any())).thenReturn(Optional.of(List.of(
                new RiotStatsGateway.Coverage(PUUID, true, 940, 0, null, null, QUAND))));

        PlayerStatsService.Figures figures = figures();

        assertThat(figures.state()).isEqualTo(StatsState.INGESTION_EN_COURS);
        assertThat(figures.coverage().pendingMatches()).isEqualTo(940);
    }

    @Test
    @DisplayName("Pool vide : aucun ratio, et surtout pas des zéros")
    void aucuneDivisionParZero() {
        StatLineDto total = figures().overall();

        assertThat(total.games()).isZero();
        assertThat(total.winRate()).isNull();
        assertThat(total.kda()).isNull();
        assertThat(total.csPerMinute()).isNull();
        assertThat(total.goldPerMinute()).isNull();
    }

    @Test
    @DisplayName("Zéro mort ne divise pas : le KDA vaut les kills et les assists")
    void kdaSansMort() {
        total(4, 4, 12, 0, 8);

        assertThat(figures().overall().kda()).isEqualTo(20.0);
    }

    @Test
    @DisplayName("Un champion se compare au reste du pool du même joueur, pas à une moyenne inventée")
    void ecartAuResteDuPool() {
        total(100, 44, 300, 200, 400);
        when(statsGateway.aggregate(any(), eq(RiotStatsGateway.Grouping.CHAMPION), any(), any()))
                .thenReturn(Optional.of(List.of(bucket("126", 20, 11, 60, 40, 80))));

        StatLineDto jayce = figures().champions().getFirst();

        assertThat(jayce.winRate()).isEqualTo(0.55);
        assertThat(jayce.label()).isEqualTo("Jayce");
        assertThat(jayce.versusRest().referenceGames()).isEqualTo(80);
        assertThat(jayce.versusRest().winRateDelta()).isCloseTo(0.1375, within(1e-9));
    }

    @Test
    @DisplayName("Un champion qui est tout le pool ne se compare à rien — et pas à zéro")
    void aucunResteAComparer() {
        total(20, 11, 60, 40, 80);
        when(statsGateway.aggregate(any(), eq(RiotStatsGateway.Grouping.CHAMPION), any(), any()))
                .thenReturn(Optional.of(List.of(bucket("126", 20, 11, 60, 40, 80))));

        assertThat(figures().champions().getFirst().versusRest()).isNull();
    }

    @Test
    @DisplayName("Aucun puuid : aucune requête au connecteur")
    void aucunPuuid() {
        assertThat(service.of(List.of(), null, 8)).isEmpty();
        assertThat(service.rankings("  ")).isEmpty();
    }

    @Test
    @DisplayName("Radar : le joueur se compare à son poste le plus joué sur la période")
    void referentielsAuPostePrincipal() {
        total(100, 50, 300, 200, 400);
        when(statsGateway.aggregate(any(), eq(RiotStatsGateway.Grouping.POSITION), any(), any()))
                .thenReturn(Optional.of(List.of(bucket("TOP", 5, 2, 10, 10, 10), bucket("JUNGLE", 30, 15, 10, 10, 10))));
        when(statsGateway.references(List.of(new RiotStatsGateway.ReferenceRequest(PUUID, "JUNGLE", null))))
                .thenReturn(Optional.of(List.of(new RiotStatsGateway.References(PUUID, "JUNGLE", "GOLD",
                        new RiotStatsGateway.Reference("GOLD", "JUNGLE", 40, 5,
                                Map.of("kda", new RiotStatsGateway.Bound(1.5, 3.5))), null))));

        var references = figures().references();

        assertThat(references.position()).isEqualTo("JUNGLE");
        assertThat(references.tier()).isEqualTo("GOLD");
        assertThat(references.league().bounds().get("kda").low()).isEqualTo(1.5);
        assertThat(references.met()).isNull();
    }

    @Test
    @DisplayName("Que de l'ARAM : les chiffres de la Faille sont vides, mais ce n'est pas « aucune partie »")
    void seulementHorsFaille() {
        when(statsGateway.coverage(any())).thenReturn(Optional.of(List.of(
                new RiotStatsGateway.Coverage(PUUID, true, 40, 40, QUAND, QUAND, QUAND))));

        assertThat(figures().state()).isEqualTo(StatsState.STATISTIQUES_CONNUES);
    }
}
