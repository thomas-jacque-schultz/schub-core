package schultz.thomas.schub.core.business.service;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.UpdateDefinition;

import schultz.thomas.schub.core.business.mapper.GameServerMapper;
import schultz.thomas.schub.core.data.model.GameServer;
import schultz.thomas.schub.core.data.model.GameServerStatus;
import schultz.thomas.schub.core.data.model.GameServerStatusHistoryEntry;
import schultz.thomas.schub.core.data.repository.GameServerRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GameServerServiceTest {

    private final GameServerRepository repository = mock(GameServerRepository.class);
    private final GameServerMapper mapper = mock(GameServerMapper.class);
    private final MongoTemplate mongo = mock(MongoTemplate.class);
    private GameServerService service;

    @BeforeEach
    void setUp() {
        service = new GameServerService(repository, mapper, mock(DiscordNotifier.class), mongo);
    }

    @Test
    @DisplayName("modifier un serveur n'écrit ni son état observé ni son historique, et garde son slug")
    void modificationSansChampsObserves() {
        GameServer stocke = serveur();
        when(repository.findById("s1")).thenReturn(Optional.of(stocke));
        when(repository.findAll()).thenReturn(List.of(stocke));
        MongoConverter converter = mock(MongoConverter.class);
        when(mongo.getConverter()).thenReturn(converter);
        doAnswer(appel -> {
            appel.getArgument(1, Document.class).append("_id", "s1").append("slug", "palworld")
                    .append("name", "Nouveau nom").append("status", "ONLINE")
                    .append("statusHistory", List.of()).append("lastStatusCheckAt", Instant.now());
            return null;
        }).when(converter).write(any(), any(Document.class));

        GameServer source = new GameServer();
        source.setSlug("autre-slug");
        service.update("s1", source);

        ArgumentCaptor<UpdateDefinition> update = ArgumentCaptor.forClass(UpdateDefinition.class);
        verify(mongo).updateFirst(any(Query.class), update.capture(), eq(GameServer.class));
        Document champs = update.getValue().getUpdateObject().get("$set", Document.class);
        assertThat(champs).containsKeys("name", "slug")
                .doesNotContainKeys("_id", "status", "statusHistory", "lastStatusCheckAt");
        assertThat(stocke.getSlug()).isEqualTo("palworld");
    }

    @Test
    @DisplayName("un changement d'état ajoute une entrée à l'historique, borné aux dernières")
    void historiqueBorne() {
        GameServer observe = serveur();
        observe.setStatus(GameServerStatus.ONLINE);
        observe.setLastStatusCheckAt(Instant.now());
        observe.getStatusHistory().add(new GameServerStatusHistoryEntry(GameServerStatus.ONLINE, Instant.now()));

        service.persistObservedState(observe, true);

        ArgumentCaptor<UpdateDefinition> update = ArgumentCaptor.forClass(UpdateDefinition.class);
        verify(mongo).updateFirst(any(Query.class), update.capture(), eq(GameServer.class));
        String ecrit = update.getValue().getUpdateObject().toString();
        assertThat(ecrit).contains("$push").contains("$slice").contains("-" + GameServerService.HISTORIQUE_MAX);
    }

    private static GameServer serveur() {
        GameServer serveur = new GameServer();
        serveur.setId("s1");
        serveur.setSlug("palworld");
        serveur.setStatusHistory(new ArrayList<>());
        return serveur;
    }
}
