package schultz.thomas.schub.core;

import io.mongock.runner.springboot.EnableMongock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// @EnableMongock obligatoire : mongock-springboot-v3 ne déclare aucune auto-configuration, sans elle
// aucune migration ne part, en silence.
@SpringBootApplication
@EnableMongock
public class SchubCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchubCoreApplication.class, args);
    }
}
