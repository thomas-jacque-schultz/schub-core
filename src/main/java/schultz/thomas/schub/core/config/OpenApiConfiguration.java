package schultz.thomas.schub.core.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class OpenApiConfiguration {

    // Jackson écrit une Duration en ISO-8601 (« PT2H36M20S ») ; sans cela le contrat la décrirait comme un objet.
    static {
        SpringDocUtils.getConfig().replaceWithSchema(Duration.class,
                new StringSchema().format("duration").example("PT2H36M20S"));
    }

    @Bean
    public OpenAPI coreOpenApi() {
        return new OpenAPI().info(new Info().title("Cœur Schub").version("0.1.0")
                .description("Le domaine de Schub et de PremadeLab, derrière le BFF."));
    }
}
