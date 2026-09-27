package schultz.thomas.schub.core.team.business.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import schultz.thomas.schub.core.business.service.RiotConnectorService;
import schultz.thomas.schub.core.business.service.RiotIdResolution;
import schultz.thomas.schub.core.business.service.RiotIdResolver;
import schultz.thomas.schub.core.business.service.UnknownRiotAccountException;
import schultz.thomas.schub.core.team.api.dto.PlayerCollectDto;
import schultz.thomas.schub.core.team.business.model.PlayerRef;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SearchedPlayerServiceTest {

    private RiotIdResolver resolver;
    private RiotStatsGateway stats;
    private RiotConnectorService connector;
    private VisitorBudget budget;
    private SearchedPlayerService service;

    @BeforeEach
    void setUp() {
        resolver = mock(RiotIdResolver.class);
        stats = mock(RiotStatsGateway.class);
        connector = mock(RiotConnectorService.class);
        budget = mock(VisitorBudget.class);
        service = new SearchedPlayerService(resolver, stats, mock(RiotChampionGateway.class), connector,
                mock(PlayerStatsService.class), mock(MyStatsService.class), mock(MyGamesService.class), budget);
        when(resolver.resolve("Le Nom-Composé", "EUW")).thenReturn(RiotIdResolution.resolved("p1", "le nom-composé", "euw"));
        when(connector.requestPreview(anyString(), anyBoolean())).thenReturn(true);
    }

    @Test
    @DisplayName("Nom-TAG se coupe au dernier tiret, et la casse canonique de Riot l'emporte")
    void slug() {
        PlayerRef joueur = service.resolve("Le Nom-Composé-EUW");

        assertThat(joueur.gameName()).isEqualTo("le nom-composé");
        assertThat(joueur.tagLine()).isEqualTo("euw");
        assertThat(joueur.puuid()).isEqualTo("p1");
        assertThatThrownBy(() -> service.resolve("SansTag")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("un Riot ID inconnu de Riot : 404 clair")
    void inconnu() {
        when(resolver.resolve("Personne", "000")).thenReturn(RiotIdResolution.notFound());

        assertThatThrownBy(() -> service.resolve("Personne-000")).isInstanceOf(UnknownRiotAccountException.class);
    }

    @Test
    @DisplayName("un joueur déjà relevé ne coûte aucun appel à Riot")
    void connu() {
        when(stats.coverage(List.of("p1"))).thenReturn(Optional.of(List.of(
                new RiotStatsGateway.Coverage("p1", true, 120, 120, null, null, Instant.now()))));

        assertThat(service.collect("Le Nom-Composé-EUW", "ip:1").lane()).isEqualTo(PlayerCollectDto.Lane.KNOWN);
        verify(connector, never()).requestPreview(anyString(), anyBoolean());
    }

    @Test
    @DisplayName("au-delà du budget : voie lente, jamais de refus")
    void voieLente() {
        when(stats.coverage(List.of("p1"))).thenReturn(Optional.of(List.of()));
        when(budget.consume(any(), any())).thenReturn(true, false);

        assertThat(service.collect("Le Nom-Composé-EUW", "ip:1").lane()).isEqualTo(PlayerCollectDto.Lane.FAST);
        assertThat(service.collect("Le Nom-Composé-EUW", "ip:1").lane()).isEqualTo(PlayerCollectDto.Lane.SLOW);
        verify(connector).requestPreview("p1", false);
        verify(connector).requestPreview("p1", true);
    }
}
