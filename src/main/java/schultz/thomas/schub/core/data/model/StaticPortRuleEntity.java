package schultz.thomas.schub.core.data.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "static_port_rules")
@CompoundIndex(name = "proto_wan_unique", def = "{'proto': 1, 'wanPortStart': 1}", unique = true)
public class StaticPortRuleEntity {

    @Id
    private String id;

    private String name;

    private String proto;

    private Integer wanPortStart;

    private Integer wanPortEnd;

    private Integer lanPort;

    private String lanIp;

    private boolean enabled = true;
}
