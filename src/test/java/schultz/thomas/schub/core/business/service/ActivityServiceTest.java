package schultz.thomas.schub.core.business.service;

import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import schultz.thomas.schub.core.data.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ActivityServiceTest {

    private final MongoTemplate mongo = mock(MongoTemplate.class);
    private final ActivityService service = new ActivityService(mongo, mock(UserRepository.class));

    @Test
    @DisplayName("Une application inconnue est comptée pour Schub, jamais créée à la volée")
    void applicationInconnue() {
        service.recordActivity("u1", "autre");

        ArgumentCaptor<Query> requete = ArgumentCaptor.forClass(Query.class);
        verify(mongo).findAndModify(requete.capture(), any(Update.class), any(FindAndModifyOptions.class),
                eq(Document.class), eq("user_activity"));
        assertThat(requete.getValue().getQueryObject().getString("_id")).endsWith("|u1|schub");
    }

    @Test
    @DisplayName("Une recherche sans visiteur n'est pas comptée")
    void sansVisiteur() {
        service.recordSearch(null);
        service.recordSearch(" ");

        verify(mongo, never()).upsert(any(Query.class), any(Update.class), anyString());
    }
}
