package schultz.thomas.schub.core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import schultz.thomas.schub.core.config.PortForwardingProperties;
import schultz.thomas.schub.core.model.GameServer;
import schultz.thomas.schub.core.model.GameServerPort;
import schultz.thomas.schub.core.model.GameServerStatus;
import schultz.thomas.schub.core.portforwarding.PortRule;
import schultz.thomas.schub.core.portforwarding.PortRuleKey;
import schultz.thomas.schub.core.portforwarding.PortRuleResolution;
import schultz.thomas.schub.core.portforwarding.Protocol;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Calcule l'ensemble des redirections voulues, à partir des règles permanentes et de l'état
 * des serveurs de jeu. Fonction pure : aucune I/O, aucun routeur, donc testable directement.
 *
 * <p>Deux sources :</p>
 * <ul>
 *   <li>les règles permanentes, toujours ouvertes tant qu'elles sont déclarées ;</li>
 *   <li>les ports portés par chaque serveur, ouverts seulement pendant qu'il tourne.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class PortRuleResolver {

    private static final String STATIC_OWNER_PREFIX = "static/";
    private static final int MIN_PORT = 1;
    private static final int MAX_PORT = 65535;

    private final PortForwardingProperties properties;
    private final GameServerService gameServerService;

    /**
     * Les règles permanentes viennent d'un fournisseur et non des propriétés : depuis qu'elles
     * sont modifiables depuis l'interface, elles vivent en base. Ce détour garde ce resolver
     * pur — il consomme une liste sans savoir d'où elle sort.
     */
    private final StaticPortRuleProvider staticPortRuleProvider;

    /**
     * @param overrideIdentifier serveur dont on force l'état au lieu de le déduire de son statut,
     *                           ou {@code null}. Nécessaire autour d'un démarrage ou d'une extinction :
     *                           Portainer n'a pas encore basculé le statut au moment où l'on veut que
     *                           la redirection soit déjà dans le bon état.
     */
    public PortRuleResolution resolve(String overrideIdentifier, boolean overrideOpen) {
        Map<PortRuleKey, PortRule> rules = new LinkedHashMap<>();
        List<String> rejected = new ArrayList<>();

        for (Candidate candidate : candidates(overrideIdentifier, overrideOpen)) {
            validate(candidate, rejected).ifPresent(rule -> {
                PortRule previous = rules.putIfAbsent(rule.key(), rule);
                if (previous != null) {
                    rejected.add(rule.owner() + ": " + rule.protocol().wireName() + "/" + rule.wanPortStart()
                            + " déjà réclamé par '" + previous.owner() + "'");
                }
            });
        }

        return new PortRuleResolution(rules, rejected);
    }

    // --- collecte -----------------------------------------------------------

    private List<Candidate> candidates(String overrideIdentifier, boolean overrideOpen) {
        List<Candidate> candidates = new ArrayList<>();

        for (PortForwardingProperties.StaticRule rule : staticPortRuleProvider.staticRules()) {
            candidates.add(new Candidate(staticOwner(rule), rule.getProto(), rule.getWanPortStart(),
                    rule.getWanPortEnd(), rule.getLanIp(), rule.getLanPort(), rule.isEnabled()));
        }

        for (GameServer server : gameServerService.findAll()) {
            if (server.getPorts() == null || server.getPorts().isEmpty()) {
                continue;
            }
            boolean open = shouldBeOpen(server, overrideIdentifier, overrideOpen);
            for (GameServerPort port : server.getPorts()) {
                candidates.add(new Candidate(server.getSlug(), port.getProto(), port.getWanPort(),
                        null, port.getLanIp(), port.getLanPort(), open));
            }
        }

        return candidates;
    }

    private boolean shouldBeOpen(GameServer server, String overrideIdentifier, boolean overrideOpen) {
        if (server.getSlug() != null && server.getSlug().equals(overrideIdentifier)) {
            return overrideOpen;
        }
        return GameServerStatus.ONLINE.equals(server.getStatus());
    }

    private String staticOwner(PortForwardingProperties.StaticRule rule) {
        String name = (rule.getName() != null && !rule.getName().isBlank())
                ? rule.getName().trim()
                : rule.getProto() + "-" + rule.getWanPortStart();
        return STATIC_OWNER_PREFIX + name;
    }

    // --- validation ---------------------------------------------------------

    /** Vide si la règle est refusée ; le motif est alors ajouté à {@code rejected}. */
    private Optional<PortRule> validate(Candidate candidate, List<String> rejected) {
        Optional<Protocol> protocol = Protocol.parse(candidate.proto());
        if (protocol.isEmpty()) {
            return reject(rejected, candidate, "protocole invalide '" + candidate.proto() + "'");
        }
        if (!isValidPort(candidate.wanPortStart())) {
            return reject(rejected, candidate, "port WAN invalide '" + candidate.wanPortStart() + "'");
        }

        int wanPortStart = candidate.wanPortStart();
        int wanPortEnd = candidate.wanPortEnd() != null ? candidate.wanPortEnd() : wanPortStart;
        if (wanPortEnd < wanPortStart || wanPortEnd > MAX_PORT) {
            return reject(rejected, candidate, "plage de ports WAN invalide " + wanPortStart + "-" + wanPortEnd);
        }

        Optional<Integer> forbidden = firstForbiddenPort(wanPortStart, wanPortEnd);
        if (forbidden.isPresent()) {
            return reject(rejected, candidate,
                    "port " + forbidden.get() + " interdit par port-forwarding.forbidden-wan-ports");
        }

        String lanIp = firstNonBlank(candidate.lanIp(), properties.getDefaultLanIp());
        if (lanIp == null) {
            return reject(rejected, candidate,
                    "aucune IP LAN cible (ni sur la règle, ni port-forwarding.default-lan-ip)");
        }

        int lanPort = candidate.lanPort() != null ? candidate.lanPort() : wanPortStart;
        if (!isValidPort(lanPort)) {
            return reject(rejected, candidate, "port LAN invalide '" + candidate.lanPort() + "'");
        }

        return Optional.of(new PortRule(null, candidate.owner(), protocol.get(),
                wanPortStart, wanPortEnd, lanIp, lanPort, candidate.open()));
    }

    private Optional<PortRule> reject(List<String> rejected, Candidate candidate, String reason) {
        rejected.add(candidate.owner() + ": " + reason);
        return Optional.empty();
    }

    private boolean isValidPort(Integer port) {
        return port != null && port >= MIN_PORT && port <= MAX_PORT;
    }

    private Optional<Integer> firstForbiddenPort(int start, int end) {
        List<Integer> forbidden = properties.getForbiddenWanPorts();
        if (forbidden == null) {
            return Optional.empty();
        }
        return forbidden.stream().filter(port -> port >= start && port <= end).findFirst();
    }

    private String firstNonBlank(String first, String fallback) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        return (fallback != null && !fallback.isBlank()) ? fallback.trim() : null;
    }

    /** Une règle telle que déclarée, avant normalisation et validation. */
    private record Candidate(
            String owner,
            String proto,
            Integer wanPortStart,
            Integer wanPortEnd,
            String lanIp,
            Integer lanPort,
            boolean open
    ) {
    }
}
