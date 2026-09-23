package schultz.thomas.schub.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "port-forwarding")
public class PortForwardingProperties {

    private boolean enabled = false;

    private boolean dryRun = false;

    private String defaultLanIp = "";

    private boolean pruneOrphans = false;

    private List<Integer> forbiddenWanPorts = new ArrayList<>(List.of(22, 23, 139, 445, 3389, 5432, 27017, 9443));

    private List<StaticRule> staticRules = new ArrayList<>();

    @Data
    public static class StaticRule {
        private String name;
        private String proto = "tcp";
        private Integer wanPortStart;
        private Integer wanPortEnd;
        private Integer lanPort;
        private String lanIp;
        private boolean enabled = true;
    }
}
