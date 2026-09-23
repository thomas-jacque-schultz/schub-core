package schultz.thomas.schub.core.data.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import schultz.thomas.schub.core.business.model.Permission;

import java.util.EnumSet;
import java.util.Set;

@Data
@Document(collection = "roles")
public class Role {

    @Id
    private String id;

    @Indexed(unique = true)
    private String name;

    private Set<Permission> permissions = EnumSet.noneOf(Permission.class);

    private boolean system;
}
