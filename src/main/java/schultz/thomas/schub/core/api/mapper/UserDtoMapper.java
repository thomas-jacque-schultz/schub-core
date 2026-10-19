package schultz.thomas.schub.core.api.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import schultz.thomas.schub.core.api.dto.AccountLinksDto;
import schultz.thomas.schub.core.api.dto.MeDto;
import schultz.thomas.schub.core.api.dto.UserDto;
import schultz.thomas.schub.core.api.dto.UserIdentityDto;
import schultz.thomas.schub.core.business.service.PermissionEvaluator;
import schultz.thomas.schub.core.business.service.RiotAccountService;
import schultz.thomas.schub.core.business.service.UserService;
import schultz.thomas.schub.core.data.model.Role;
import schultz.thomas.schub.core.data.model.SessionSummaryPreferences;
import schultz.thomas.schub.core.data.model.User;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class UserDtoMapper {

    private final UserService userService;
    private final PermissionEvaluator permissionEvaluator;
    private final RiotAccountService riotAccountService;
    private final AccountDtoMapper accounts;

    public MeDto toMeDto(User user) {
        Role role = userService.roleOf(user).orElse(null);
        return new MeDto(
                user.getId(),
                new MeDto.DiscordIdentityDto(user.getDiscordId(), user.getDiscordUsername(), user.getAvatarUrl()),
                userService.displayNameOf(user),
                user.getDisplayName() != null && !user.getDisplayName().isBlank(),
                new MeDto.RoleSummaryDto(role == null ? null : role.getName(),
                        permissionEvaluator.rolePermissions(user)),
                accounts.toDto(riotAccountService.of(user)),
                AccountLinksDto.of(user),
                new MeDto.SessionSummariesDto(SessionSummaryPreferences.of(user).planned(),
                        SessionSummaryPreferences.of(user).free()),
                user.getDmUnreachableAt() != null);
    }

    public UserIdentityDto toIdentityDto(User user) {
        return new UserIdentityDto(toDto(user), permissionEvaluator.rolePermissions(user));
    }

    public UserDto toDto(User user) {
        return toDto(user, userService.roleOf(user).map(Role::getName).orElse(null));
    }

    public List<UserDto> toDtos(List<User> users) {
        Map<String, String> roleNames = userService.roleNamesById();
        return users.stream()
                .map(user -> toDto(user, roleNames.get(user.getRoleId())))
                .toList();
    }

    private UserDto toDto(User user, String roleName) {
        return new UserDto(
                user.getId(),
                user.getDiscordId(),
                user.getDiscordUsername(),
                userService.displayNameOf(user),
                user.getAvatarUrl(),
                user.getRoleId(),
                roleName,
                user.getRiotGameName(),
                user.getRiotTagLine(),
                user.getCreatedAt(),
                user.getLastLoginAt(),
                AccountLinksDto.of(user));
    }
}
