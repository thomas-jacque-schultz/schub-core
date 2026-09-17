package schultz.thomas.schub.core;

import schultz.thomas.schub.core.data.model.GameServer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Domaine Schub : GameServer, Deployment, politique de ports. Orchestre les connecteurs.
 */
@SpringBootApplication
public class SchubCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchubCoreApplication.class, args);
    }
}
