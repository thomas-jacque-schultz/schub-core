package schultz.thomas.schub.core.data.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String discordId;

    private String discordUsername;
    private String avatarUrl;

    // null tant que non choisi, pour que le pseudo Discord continue de suivre. Pas unique : discordId est la seule clé.
    private String displayName;

    private String roleId;

    private String riotPuuid;
    private String riotGameName;
    private String riotTagLine;

    private Instant riotLinkedAt;

    private Instant createdAt;
    private Instant lastLoginAt;
}
