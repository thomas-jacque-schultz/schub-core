package schultz.thomas.schub.core.team.business.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * La passerelle réelle : deux routes du connecteur Riot, et rien d'autre.
 *
 * <p>Le cœur ne parle pas à l'API Riot et ne doit pas l'apprendre — c'est le connecteur qui
 * détient la clé, le quota et le cache (plan §D.1). Ici, ce sont deux appels locaux sur
 * l'overlay, portant le secret interne du maillage.</p>
 *
 * <p><strong>Toute erreur devient un {@link Optional#empty()}.</strong> Connecteur éteint, clé de
 * développement expirée, quota épuisé, {@code puuid} inconnu de Riot : ces quatre cas appellent
 * la même conduite côté panneau — dire que la donnée manque, et laisser le reste de la page
 * s'afficher. Les distinguer demanderait de transporter un statut HTTP jusqu'à l'écran pour lui
 * faire écrire quatre phrases là où une suffit ; le jour où une seule mérite un traitement
 * propre, c'est cette classe qu'il faudra rouvrir.</p>
 */
@Slf4j
@Service
public class ConnectorRiotChampionGateway implements RiotChampionGateway {

    private static final ParameterizedTypeReference<List<MasteryResponse>> MAITRISES =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;

    public ConnectorRiotChampionGateway(@Qualifier("connectorRiotRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Optional<Catalogue> catalogue() {
        try {
            CatalogueResponse reponse = restClient.get()
                    .uri("/champions")
                    .retrieve()
                    .body(CatalogueResponse.class);
            if (reponse == null || reponse.version() == null || reponse.champions() == null) {
                log.warn("Catalogue des champions vide ou sans version — traité comme indisponible");
                return Optional.empty();
            }
            return Optional.of(new Catalogue(reponse.version(), indexe(reponse.champions())));
        } catch (RestClientException e) {
            log.warn("Catalogue des champions non obtenu ({}) — le pool sera servi sans champions",
                    e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<List<Mastery>> masteries(String puuid, int limit) {
        if (puuid == null || puuid.isBlank()) {
            return Optional.empty();
        }
        try {
            List<MasteryResponse> reponse = restClient.get()
                    .uri(uri -> uri.path("/players/{puuid}/champion-mastery")
                            .queryParamIfPresent("limit",
                                    limit > 0 ? java.util.Optional.of(limit) : java.util.Optional.empty())
                            .build(puuid))
                    .retrieve()
                    .body(MAITRISES);
            if (reponse == null) {
                return Optional.empty();
            }
            return Optional.of(reponse.stream()
                    .map(m -> new Mastery(m.championId(), m.level(), m.points(),
                            m.lastPlayedAt(), m.observedAt()))
                    .toList());
        } catch (RestClientException e) {
            log.warn("Maîtrises non obtenues pour un joueur ({}) — sa colonne dira pourquoi elle est vide",
                    e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Le dernier gagne si deux entrées portaient le même identifiant numérique — Data Dragon ne
     * le fait pas, et un doublon ne justifierait pas de faire échouer tout un panneau.
     */
    private static Map<Integer, Champion> indexe(List<ChampionResponse> champions) {
        Map<Integer, Champion> parId = new LinkedHashMap<>();
        for (ChampionResponse champion : champions) {
            if (champion != null) {
                parId.put(champion.id(),
                        new Champion(champion.id(), champion.key(), champion.name(), champion.iconUrl()));
            }
        }
        return Map.copyOf(parId);
    }

    // --- les formes rendues par le connecteur ---
    //
    // Redéclarées ici plutôt que partagées : deux services ne partagent pas de classes, c'est ce
    // qui leur permet d'évoluer séparément (migration §5). Les champs non lus — `locale`,
    // `title`, `tags`, `championName` — sont ignorés à la désérialisation. `championName` en
    // particulier n'est pas repris : le nom vient du catalogue, qui porte la version qui lui
    // donne son sens, et le prendre ailleurs le détacherait de son patch.

    record CatalogueResponse(String version, String locale, List<ChampionResponse> champions) {
    }

    record ChampionResponse(String key, int id, String name, String iconUrl) {
    }

    record MasteryResponse(int championId, String championName, int level, int points,
                           Instant lastPlayedAt, Instant observedAt) {
    }
}
