package schultz.thomas.schub.core.team.business.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import schultz.thomas.schub.core.business.service.RiotConnectorUnavailableException;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.team.api.dto.GamePlayerMetricsDto;
import schultz.thomas.schub.core.team.api.dto.MyGamesDto;
import schultz.thomas.schub.core.team.api.dto.TeamGameDetailDto;
import schultz.thomas.schub.core.team.business.model.StatsState;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MyGamesServiceTest {

    private static final String MOI = "puuid-moi";

    @Mock private RiotStatsGateway statsGateway;
    @Mock private RiotChampionGateway championGateway;
    @Mock private MemberDirectory memberDirectory;

    private MyGamesService service;
    private User actor;

    @BeforeEach
    void setUp() {
        service = new MyGamesService(statsGateway, memberDirectory, new GameViews(statsGateway, championGateway));
        actor = new User();
        actor.setId("user-1");
        actor.setRiotPuuid(MOI);
        actor.setRiotGameName("Moi");
        actor.setRiotTagLine("EUW");
        lenient().when(memberDirectory.byIds(any())).thenReturn(Map.of());
        lenient().when(championGateway.catalogue()).thenReturn(Optional.empty());
        lenient().when(statsGateway.insights(any())).thenReturn(Optional.of(List.of()));
    }

    @Test
    @DisplayName("sans compte Riot, l'historique le dit au lieu d'appeler le connecteur")
    void sansCompte() {
        actor.setRiotPuuid(null);

        assertThat(service.games(actor, null, null).state()).isEqualTo(StatsState.COMPTE_RIOT_ABSENT);
        verify(statsGateway, never()).playerMatches(any(), any(), any());
    }

    @Test
    @DisplayName("la liste ne demande aucun enrichissement : c'est l'ouverture d'une partie qui le fait")
    void listeSansEnrichissement() {
        when(statsGateway.playerMatches(eq(MOI), any(), eq(MyGamesService.PARTIES_DEFAUT)))
                .thenReturn(Optional.of(new RiotStatsGateway.SharedMatches(1, 1, false, List.of(partie()))));

        MyGamesDto historique = service.games(actor, null, null);

        assertThat(historique.state()).isEqualTo(StatsState.STATISTIQUES_CONNUES);
        assertThat(historique.games()).singleElement().satisfies(partie -> {
            assertThat(partie.players()).singleElement().satisfies(joueur -> {
                assertThat(joueur.memberId()).isEqualTo("user-1");
                assertThat(joueur.displayName()).isEqualTo("Moi");
            });
            assertThat(partie.enemies()).hasSize(1);
        });
        verify(statsGateway, never()).sharedMatches(any(), anyInt(), any(), any());
    }

    @Test
    @DisplayName("connecteur muet : l'état le dit, la liste ne se vide pas en silence")
    void connecteurMuet() {
        when(statsGateway.playerMatches(any(), any(), any())).thenReturn(Optional.empty());

        assertThat(service.games(actor, null, null).state()).isEqualTo(StatsState.CONNECTEUR_INDISPONIBLE);
    }

    @Test
    @DisplayName("une partie qui n'est pas la sienne n'a pas de détail")
    void partieEtrangere() {
        when(statsGateway.sharedMatchesAmong(List.of(MOI), 1, List.of("EUW1_9")))
                .thenReturn(Optional.of(new RiotStatsGateway.SharedMatches(1, 0, false, List.of())));

        assertThatThrownBy(() -> service.game(actor, "EUW1_9", null)).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("sans réponse du connecteur, le détail refuse plutôt que de dire « pas à toi »")
    void detailConnecteurMuet() {
        when(statsGateway.sharedMatchesAmong(any(), anyInt(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.game(actor, "EUW1_1", null))
                .isInstanceOf(RiotConnectorUnavailableException.class);
    }

    @Test
    @DisplayName("chaque joueur du détail porte la partie et sa moyenne au poste qu'il y tenait")
    void indicateursDuDetail() {
        when(statsGateway.sharedMatchesAmong(List.of(MOI), 1, List.of("EUW1_1")))
                .thenReturn(Optional.of(new RiotStatsGateway.SharedMatches(1, 1, false, List.of(partie()))));
        when(statsGateway.matchMetrics("EUW1_1")).thenReturn(Optional.of(List.of(
                new RiotStatsGateway.PlayerMetrics(MOI, 100, "MIDDLE", 1, "GOLD", false,
                        Map.of("csPerMinute", 8.0)),
                new RiotStatsGateway.PlayerMetrics("puuid-eux", 200, "MIDDLE", 2, null, false,
                        Map.of("csPerMinute", 6.0)))));
        when(statsGateway.aggregate(any(), eq(RiotStatsGateway.Grouping.POSITION), eq(RiotStatsGateway.Scope.RIFT),
                any())).thenReturn(Optional.of(List.of(
                        moyenne(MOI, "MIDDLE", 10, 7 * 600, 36_000),
                        moyenne(MOI, "TOP", 3, 30, 5_400))));

        TeamGameDetailDto detail = service.game(actor, "EUW1_1", 30);

        assertThat(detail.days()).isEqualTo(30);
        assertThat(detail.viewerMemberId()).isEqualTo("user-1");
        GamePlayerMetricsDto moi = detail.metrics().getFirst();
        assertThat(moi.game()).containsEntry("csPerMinute", 8.0);
        assertThat(moi.tier()).isEqualTo("GOLD");
        assertThat(moi.average().key()).isEqualTo("MIDDLE");
        assertThat(moi.average().csPerMinute()).isEqualTo(7.0);
        assertThat(detail.metrics().get(1).average()).isNull();
    }

    private static RiotStatsGateway.SharedMatch partie() {
        return new RiotStatsGateway.SharedMatch("EUW1_1", Instant.parse("2026-09-20T18:00:00Z"), 1800, 420,
                "RANKED_SOLO", "16.19", 1, false, true,
                List.of(joueur(MOI, 100, true)), List.of(joueur("puuid-eux", 200, false)));
    }

    private static RiotStatsGateway.SharedMatchPlayer joueur(String puuid, int side, boolean win) {
        return new RiotStatsGateway.SharedMatchPlayer(puuid, side == 100 ? 1 : 2, "Ahri", "MIDDLE", win, side,
                5, 2, 7, 240, 12_000, 20_000, 15_000, 30, false);
    }

    private static RiotStatsGateway.Bucket moyenne(String puuid, String poste, long parties, long minions,
                                                   long secondes) {
        return new RiotStatsGateway.Bucket(puuid, poste, null, parties, parties / 2, 0, 0, 0, minions, 0, 0, 0, 0,
                0, 0, 0, secondes, null, null, null);
    }
}
