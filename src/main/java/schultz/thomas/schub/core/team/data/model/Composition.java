package schultz.thomas.schub.core.team.data.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "team_compositions")
public class Composition {

    @Id
    private String id;

    @Indexed
    private String teamId;

    private String name;

    private List<CompositionSlot> slots = new ArrayList<>();

    private List<String> bans = new ArrayList<>();

    private String patch;

    private String notes;

    private String createdBy;

    private Instant createdAt;
    private Instant updatedAt;
}
