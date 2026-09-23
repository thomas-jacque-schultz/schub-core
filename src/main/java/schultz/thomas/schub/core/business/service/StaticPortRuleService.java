package schultz.thomas.schub.core.business.service;

import schultz.thomas.schub.core.api.dto.Protocol;
import schultz.thomas.schub.core.config.PortForwardingProperties;
import schultz.thomas.schub.core.data.model.StaticPortRuleEntity;
import schultz.thomas.schub.core.data.repository.StaticPortRuleRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StaticPortRuleService implements StaticPortRuleProvider {

    private static final int MIN_PORT = 1;
    private static final int MAX_PORT = 65535;

    private final StaticPortRuleRepository repository;
    private final PortForwardingProperties properties;

    @PostConstruct
    void seedFromConfigurationIfEmpty() {
        List<PortForwardingProperties.StaticRule> fromFile = properties.getStaticRules();
        if (fromFile == null || fromFile.isEmpty() || repository.count() > 0) {
            return;
        }

        log.info("Base de règles permanentes vide : reprise des {} règle(s) du fichier de configuration", fromFile.size());
        for (PortForwardingProperties.StaticRule rule : fromFile) {
            StaticPortRuleEntity entity = new StaticPortRuleEntity();
            entity.setName(rule.getName());
            entity.setProto(rule.getProto());
            entity.setWanPortStart(rule.getWanPortStart());
            entity.setWanPortEnd(rule.getWanPortEnd());
            entity.setLanPort(rule.getLanPort());
            entity.setLanIp(rule.getLanIp());
            entity.setEnabled(rule.isEnabled());
            try {
                repository.save(entity);
            } catch (DuplicateKeyException e) {
                log.warn("Règle '{}' ignorée à la reprise : doublon sur {}/{}",
                        rule.getName(), rule.getProto(), rule.getWanPortStart());
            }
        }
    }

    public List<StaticPortRuleEntity> findAll() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(StaticPortRuleEntity::getWanPortStart,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(StaticPortRuleEntity::getProto, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    public StaticPortRuleEntity create(StaticPortRuleEntity rule) {
        validate(rule);

        String proto = rule.getProto().trim().toLowerCase();
        rule.setId(null);
        rule.setProto(proto);
        rule.setName(rule.getName().trim());

        repository.findByProtoIgnoreCaseAndWanPortStart(proto, rule.getWanPortStart())
                .ifPresent(existing -> {
                    throw new IllegalStateException("Une règle permanente existe déjà sur "
                            + proto + "/" + rule.getWanPortStart() + " (« " + existing.getName() + " »)");
                });

        try {
            return repository.save(rule);
        } catch (DuplicateKeyException e) {
            throw new IllegalStateException("Une règle permanente existe déjà sur "
                    + proto + "/" + rule.getWanPortStart());
        }
    }

    public boolean delete(String id) {
        if (!repository.existsById(id)) {
            return false;
        }
        repository.deleteById(id);
        return true;
    }

    @Override
    public List<PortForwardingProperties.StaticRule> staticRules() {
        return findAll().stream().map(entity -> {
            PortForwardingProperties.StaticRule rule = new PortForwardingProperties.StaticRule();
            rule.setName(entity.getName());
            rule.setProto(entity.getProto());
            rule.setWanPortStart(entity.getWanPortStart());
            rule.setWanPortEnd(entity.getWanPortEnd());
            rule.setLanPort(entity.getLanPort());
            rule.setLanIp(entity.getLanIp());
            rule.setEnabled(entity.isEnabled());
            return rule;
        }).toList();
    }

    private void validate(StaticPortRuleEntity rule) {
        if (rule.getName() == null || rule.getName().isBlank()) {
            throw new IllegalArgumentException("Un libellé est obligatoire : c'est lui qui identifie la règle sur le routeur");
        }
        if (Protocol.parse(rule.getProto()).isEmpty()) {
            throw new IllegalArgumentException("Protocole invalide : attendu 'tcp' ou 'udp'");
        }
        if (!isValidPort(rule.getWanPortStart())) {
            throw new IllegalArgumentException("Port WAN invalide : attendu entre " + MIN_PORT + " et " + MAX_PORT);
        }
        if (rule.getWanPortEnd() != null
                && (rule.getWanPortEnd() < rule.getWanPortStart() || rule.getWanPortEnd() > MAX_PORT)) {
            throw new IllegalArgumentException("Plage de ports WAN invalide");
        }
        if (rule.getLanPort() != null && !isValidPort(rule.getLanPort())) {
            throw new IllegalArgumentException("Port LAN invalide");
        }

        int end = rule.getWanPortEnd() != null ? rule.getWanPortEnd() : rule.getWanPortStart();
        List<Integer> forbidden = properties.getForbiddenWanPorts();
        if (forbidden != null) {
            forbidden.stream()
                    .filter(port -> port >= rule.getWanPortStart() && port <= end)
                    .findFirst()
                    .ifPresent(port -> {
                        throw new IllegalArgumentException("Le port " + port
                                + " ne peut pas être exposé sur Internet (port-forwarding.forbidden-wan-ports)");
                    });
        }

        boolean noTarget = (rule.getLanIp() == null || rule.getLanIp().isBlank())
                && (properties.getDefaultLanIp() == null || properties.getDefaultLanIp().isBlank());
        if (noTarget) {
            throw new IllegalArgumentException("Aucune IP LAN cible, et aucune valeur par défaut configurée");
        }
    }

    private boolean isValidPort(Integer port) {
        return port != null && port >= MIN_PORT && port <= MAX_PORT;
    }
}
