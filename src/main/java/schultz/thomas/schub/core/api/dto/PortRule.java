package schultz.thomas.schub.core.api.dto;

import java.util.Objects;

// Copie du PortRule du connecteur Freebox, à garder identique (pas de jar partagé).
public record PortRule(
        String providerId,
        String owner,
        Protocol protocol,
        int wanPortStart,
        int wanPortEnd,
        String lanIp,
        int lanPort,
        boolean open
) {

    public boolean managed() {
        return owner != null;
    }

    public PortRuleKey key() {
        return new PortRuleKey(protocol, wanPortStart, wanPortEnd);
    }

    public boolean hasSameRoutingAs(PortRule other) {
        return other != null
                && Objects.equals(lanIp, other.lanIp)
                && lanPort == other.lanPort
                && Objects.equals(owner, other.owner);
    }

    public PortRule withProviderId(String id) {
        return new PortRule(id, owner, protocol, wanPortStart, wanPortEnd, lanIp, lanPort, open);
    }

    public PortRule withOpen(boolean newOpen) {
        return new PortRule(providerId, owner, protocol, wanPortStart, wanPortEnd, lanIp, lanPort, newOpen);
    }

    public String describe() {
        String range = wanPortStart == wanPortEnd
                ? String.valueOf(wanPortStart)
                : wanPortStart + "-" + wanPortEnd;
        return (owner != null ? owner : "(manuelle)") + " " + protocol.wireName() + "/" + range;
    }
}
