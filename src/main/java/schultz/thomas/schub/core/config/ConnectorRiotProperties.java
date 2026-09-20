package schultz.thomas.schub.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Comment joindre le connecteur Riot.
 *
 * <p>Le cœur ne lui demande qu'une chose au lot D.4 : résoudre un {@code Pseudo#TAG} en
 * {@code puuid} à l'ajout d'un membre. Les délais sont courts exprès — cet appel est au mieux,
 * et un ajout de membre ne doit pas attendre dix secondes une réponse dont il peut se passer.</p>
 */
@Data
@ConfigurationProperties(prefix = "connector.riot")
public class ConnectorRiotProperties {

    private String baseUrl = "http://connector-riot:8080";

    private Duration connectTimeout = Duration.ofSeconds(2);

    private Duration readTimeout = Duration.ofSeconds(5);
}
