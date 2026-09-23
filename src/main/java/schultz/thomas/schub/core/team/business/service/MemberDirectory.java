package schultz.thomas.schub.core.team.business.service;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

// Seul point de contact avec l'identité : devient un client HTTP vers /users si team sort du cœur.
// Ni jointure sur User, ni pseudo recopié dans un document d'équipe.
public interface MemberDirectory {

    record MemberIdentity(
            String userId,
            String displayName,
            String avatarUrl,
            String riotPuuid,
            String riotGameName,
            String riotTagLine
    ) {
    }

    Map<String, MemberIdentity> byIds(Set<String> userIds);

    Optional<MemberIdentity> byRiotPuuid(String riotPuuid);
}
