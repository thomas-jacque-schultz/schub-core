package schultz.thomas.schub.core.lol.team.business.service;

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

    // Compte qui a déclaré ce Riot ID et attend encore son puuid (changement de clé, connecteur occupé).
    Optional<MemberIdentity> pendingByRiotId(String riotGameName, String riotTagLine);

    void adoptPuuid(String userId, String riotPuuid);
}
