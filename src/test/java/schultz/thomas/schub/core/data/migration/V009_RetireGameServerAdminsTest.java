package schultz.thomas.schub.core.data.migration;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.codecs.configuration.CodecRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class V009_RetireGameServerAdminsTest {

    @Test
    @DisplayName("la migration ne touche que les documents portant le champ, et ne fait que le retirer")
    void retraitCible() {
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        @SuppressWarnings("unchecked")
        MongoCollection<Document> servers = mock(MongoCollection.class);
        UpdateResult resultat = mock(UpdateResult.class);
        when(mongoTemplate.getCollection("servers")).thenReturn(servers);
        when(servers.updateMany(any(Bson.class), any(Bson.class))).thenReturn(resultat);
        when(resultat.getModifiedCount()).thenReturn(2L);

        new V009_RetireGameServerAdmins().execution(mongoTemplate);

        ArgumentCaptor<Bson> filtre = ArgumentCaptor.forClass(Bson.class);
        ArgumentCaptor<Bson> maj = ArgumentCaptor.forClass(Bson.class);
        verify(servers).updateMany(filtre.capture(), maj.capture());

        assertThat(rendu(filtre.getValue())).isEqualTo(rendu(Filters.exists("admins")));
        assertThat(rendu(maj.getValue())).isEqualTo(rendu(Updates.unset("admins")));
    }

    @Test
    @DisplayName("rejouée sur une base déjà migrée, elle ne modifie aucun document")
    void rejouableSansEffet() {
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        @SuppressWarnings("unchecked")
        MongoCollection<Document> servers = mock(MongoCollection.class);
        UpdateResult resultat = mock(UpdateResult.class);
        when(mongoTemplate.getCollection("servers")).thenReturn(servers);
        when(servers.updateMany(any(Bson.class), any(Bson.class))).thenReturn(resultat);
        when(resultat.getModifiedCount()).thenReturn(2L, 0L);

        V009_RetireGameServerAdmins migration = new V009_RetireGameServerAdmins();
        migration.execution(mongoTemplate);
        migration.execution(mongoTemplate);

        verify(servers, times(2)).updateMany(any(Bson.class), any(Bson.class));
        assertThat(resultat.getModifiedCount()).isZero();
    }

    private static BsonDocument rendu(Bson bson) {
        CodecRegistry registry = com.mongodb.MongoClientSettings.getDefaultCodecRegistry();
        return bson.toBsonDocument(BsonDocument.class, registry);
    }
}
