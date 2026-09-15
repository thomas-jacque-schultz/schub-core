package schultz.thomas.schub.core.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import schultz.thomas.schub.core.mapper.GameServerMapper;
import schultz.thomas.schub.core.model.GameServer;
import schultz.thomas.schub.core.repository.GameServerRepository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Le domaine : les GameServer, leur cycle de vie et leur état observé.
 *
 * <p>Un cache mémoire évite de relire Mongo à chaque passage de la boucle d'observation. Il est
 * chargé au démarrage et maintenu par ce service ; toute écriture passe donc par ici, sans quoi
 * le cache et la base divergeraient silencieusement.</p>
 */
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

    /**
     * Met à jour la fiche sans toucher à l'état observé.
     *
     * <p>Le slug n'est volontairement pas modifiable : il est le propriétaire des règles de ports
     * et l'argument des commandes Discord. Le changer orphelinerait les redirections existantes
     * et casserait les commandes déjà connues des utilisateurs.</p>
     */
    public GameServer update(String id, GameServer source) {
        GameServer target = findById(id)
                .orElseThrow(() -> new NoSuchElementException("Aucun serveur d'identifiant '" + id + "'"));
        mapper.updateFromSource(source, target);
        GameServer saved = repository.save(target);
        reloadCache();
        discordNotifier.gameServerChanged(saved.getSlug());
        return saved;
    }

    /** Enregistre un état observé par la boucle, sans notifier : c'est à l'appelant de décider. */
    public void persistObservedState(GameServer gameServer) {
        repository.save(gameServer);
    }

    private void reloadCache() {
        cache = repository.findAll();
    }
}
