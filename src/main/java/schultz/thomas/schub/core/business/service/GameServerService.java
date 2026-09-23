package schultz.thomas.schub.core.business.service;

import schultz.thomas.schub.core.business.mapper.GameServerMapper;
import schultz.thomas.schub.core.data.model.GameServer;
import schultz.thomas.schub.core.data.model.GameServerStatusHistoryEntry;
import schultz.thomas.schub.core.data.repository.GameServerRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

// La boucle d'observation et les modifications écrivent chacune leurs champs : aucune ne remplace le document.
@Slf4j
@Service
@RequiredArgsConstructor
public class GameServerService {

    static final int HISTORIQUE_MAX = 200;
    private static final Set<String> CHAMPS_OBSERVES =
            Set.of("_id", "_class", "status", "lastStatusCheckAt", "lastStatusChangeAt", "statusHistory");

    private final GameServerRepository repository;
    private final GameServerMapper mapper;
    private final DiscordNotifier discordNotifier;
    private final MongoTemplate mongo;

    private volatile List<GameServer> cache = List.of();

    @PostConstruct
    void loadCache() {
        cache = repository.findAll();
        log.info("{} GameServer chargés", cache.size());
    }

    public List<GameServer> findAll() {
        return cache;
    }

    public Optional<GameServer> findBySlug(String slug) {
        return cache.stream().filter(server -> slug.equals(server.getSlug())).findFirst();
    }

    public GameServer requireBySlug(String slug) {
        return findBySlug(slug)
                .orElseThrow(() -> new NoSuchElementException("Aucun serveur de slug '" + slug + "'"));
    }

    public Optional<GameServer> findById(String id) {
        return cache.stream().filter(server -> id.equals(server.getId())).findFirst();
    }

    public GameServer create(GameServer candidate) {
        if (candidate.getSlug() == null || candidate.getSlug().isBlank()) {
            throw new IllegalArgumentException("Un slug est obligatoire : il identifie le serveur partout ailleurs");
        }
        if (findBySlug(candidate.getSlug()).isPresent()) {
            throw new IllegalStateException("Un serveur porte déjà le slug '" + candidate.getSlug() + "'");
        }
        GameServer saved = repository.save(candidate);
        reloadCache();
        discordNotifier.gameServerChanged(saved.getSlug());
        return saved;
    }

    // Slug non modifiable : propriétaire des règles de ports et argument des commandes Discord.
    public GameServer update(String id, GameServer source) {
        GameServer target = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Aucun serveur d'identifiant '" + id + "'"));
        String slug = target.getSlug();
        mapper.updateFromSource(source, target);
        target.setSlug(slug);

        Document champs = new Document();
        mongo.getConverter().write(target, champs);
        Update update = new Update();
        champs.forEach((cle, valeur) -> {
            if (!CHAMPS_OBSERVES.contains(cle)) {
                update.set(cle, valeur);
            }
        });
        mongo.updateFirst(Query.query(Criteria.where("_id").is(id)), update, GameServer.class);
        reloadCache();
        discordNotifier.gameServerChanged(slug);
        return findById(id).orElse(target);
    }

    public void persistObservedState(GameServer gameServer, boolean changed) {
        Update update = new Update().set("lastStatusCheckAt", gameServer.getLastStatusCheckAt());
        if (changed) {
            List<GameServerStatusHistoryEntry> historique = gameServer.getStatusHistory();
            update.set("status", gameServer.getStatus())
                    .set("lastStatusChangeAt", gameServer.getLastStatusChangeAt())
                    .push("statusHistory").slice(-HISTORIQUE_MAX).each(historique.getLast());
        }
        mongo.updateFirst(Query.query(Criteria.where("_id").is(gameServer.getId())), update, GameServer.class);
    }

    private void reloadCache() {
        cache = repository.findAll();
    }
}
