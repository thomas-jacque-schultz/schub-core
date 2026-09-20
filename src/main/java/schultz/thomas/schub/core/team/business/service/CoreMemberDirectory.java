package schultz.thomas.schub.core.team.business.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import schultz.thomas.schub.core.business.service.UserService;
import schultz.thomas.schub.core.data.model.User;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * L'implémentation tant que le domaine d'équipe vit dans le cœur : une lecture locale des
 * comptes.
 *
 * <p><strong>C'est la seule classe de ce paquet qui connaisse le domaine de l'identité</strong>,
 * et c'est tout l'intérêt de l'avoir isolée : le jour de l'extraction, on la remplace par un
 * client HTTP et on ne relit rien d'autre. La flèche va bien dans ce sens-là — le domaine
 * d'équipe lit l'identité, jamais l'inverse.</p>
 */
@Service
@RequiredArgsConstructor
public class CoreMemberDirectory implements MemberDirectory {

    private final UserService userService;

    @Override
    public Map<String, MemberIdentity> byIds(Set<String> userIds) {
        return userService.byIds(userIds).values().stream()
                .collect(Collectors.toMap(User::getId, CoreMemberDirectory::toIdentity));
    }

    @Override
    public Optional<MemberIdentity> byRiotPuuid(String riotPuuid) {
        return userService.findByRiotPuuid(riotPuuid).map(CoreMemberDirectory::toIdentity);
    }

    private static MemberIdentity toIdentity(User user) {
        return new MemberIdentity(
                user.getId(),
                user.getDiscordUsername(),
                user.getAvatarUrl(),
                user.getRiotPuuid(),
                user.getRiotGameName(),
                user.getRiotTagLine());
    }
}
