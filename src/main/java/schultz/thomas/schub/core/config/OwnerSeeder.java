package schultz.thomas.schub.core.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import schultz.thomas.schub.core.business.model.SystemRole;
import schultz.thomas.schub.core.data.model.Role;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.data.repository.RoleRepository;
import schultz.thomas.schub.core.data.repository.UserRepository;

import java.time.Instant;
import java.util.EnumSet;

/**
 * Garde-fou anti-verrouillage : à chaque démarrage, DISCORD_ADMIN_ID est OWNER. Indépendant de V003
 * (ordre des runners non garanti, V003 ne tourne qu'une fois) : recrée le rôle s'il manque.
 */
@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@RequiredArgsConstructor
public class OwnerSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Value("${discord.admin.id:}")
    private String adminDiscordId;

    @Value("${discord.admin.username:}")
    private String adminDiscordUsername;

    @Override
    public void run(String... args) {
        if (adminDiscordId == null || adminDiscordId.isBlank()) {
            // Pas d'exception : un cœur qui refuse de démarrer emporte les serveurs de jeu avec lui.
            log.error("DISCORD_ADMIN_ID n'est pas renseigné : AUCUN compte OWNER ne sera semé. "
                    + "Le garde-fou anti-verrouillage est inactif — corrigez l'environnement.");
            return;
        }

        Role owner = ensureOwnerRole();
        User user = userRepository.findByDiscordId(adminDiscordId).orElseGet(() -> {
            User created = new User();
            created.setDiscordId(adminDiscordId);
            created.setCreatedAt(Instant.now());
            log.info("Compte OWNER créé pour l'identifiant Discord {}", adminDiscordId);
            return created;
        });

        if (adminDiscordUsername != null && !adminDiscordUsername.isBlank()
                && user.getDiscordUsername() == null) {
            user.setDiscordUsername(adminDiscordUsername);
        }
        if (!owner.getId().equals(user.getRoleId())) {
            log.warn("Le compte {} n'était pas {} — rôle rétabli au démarrage",
                    adminDiscordId, SystemRole.OWNER.roleName());
            user.setRoleId(owner.getId());
        }
        userRepository.save(user);
    }

    private Role ensureOwnerRole() {
        Role owner = roleRepository.findByName(SystemRole.OWNER.roleName()).orElseGet(() -> {
            Role created = new Role();
            created.setName(SystemRole.OWNER.roleName());
            created.setSystem(true);
            log.warn("Le rôle {} était absent — recréé", SystemRole.OWNER.roleName());
            return created;
        });
        if (!owner.getPermissions().containsAll(SystemRole.OWNER.permissions())) {
            owner.setPermissions(EnumSet.copyOf(SystemRole.OWNER.permissions()));
        }
        owner.setSystem(true);
        return roleRepository.save(owner);
    }
}
