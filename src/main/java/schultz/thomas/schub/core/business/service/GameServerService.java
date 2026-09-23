package schultz.thomas.schub.core.business.service;

import schultz.thomas.schub.core.business.mapper.GameServerMapper;
import schultz.thomas.schub.core.data.model.GameServer;
import schultz.thomas.schub.core.data.repository.GameServerRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameServerService {

    private final GameServerRepository repository;
    private final GameServerMapper mapper;
    private final DiscordNotifier discordNotifier;

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
        GameServer target = findById(id)
                .orElseThrow(() -> new NoSuchElementException("Aucun serveur d'identifiant '" + id + "'"));
        mapper.updateFromSource(source, target);
        GameServer saved = repository.save(target);
        reloadCache();
        discordNotifier.gameServerChanged(saved.getSlug());
        return saved;
    }

    public void persistObservedState(GameServer gameServer) {
        repository.save(gameServer);
    }

    private void reloadCache() {
        cache = repository.findAll();
    }
}
