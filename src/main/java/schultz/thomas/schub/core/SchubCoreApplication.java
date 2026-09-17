package schultz.thomas.schub.core;

import io.mongock.runner.springboot.EnableMongock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Domaine Schub : GameServer, Deployment, politique de ports. Orchestre les connecteurs.
 *
 * <p>{@code @EnableMongock} est <strong>obligatoire</strong> : le module
 * {@code mongock-springboot-v3} ne déclare aucune auto-configuration — ni {@code spring.factories},
 * ni {@code AutoConfiguration.imports}. Sans cette annotation, les propriétés {@code mongock.*}
 * sont lues sans effet et <em>aucune migration ne part</em>, en silence. Même piège que
 * {@code @EnableScheduling}.</p>
 */
@SpringBootApplication
@EnableMongock
public class SchubCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchubCoreApplication.class, args);
    }
}
