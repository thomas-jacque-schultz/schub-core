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
 * Le garde-fou anti-verrouillage : à <strong>chaque démarrage</strong>, le compte désigné par
 * {@code DISCORD_ADMIN_ID} est {@code OWNER}.
 *
 * <p>C'est la condition qui rend la suppression du compte local (lot A.6) acceptable. Sans porte
 * de service, une erreur de rôle — la sienne, celle d'un autre, ou un rôle mal composé — fermerait
 * l'administration à tout le monde, définitivement. Ici, redémarrer le service la rouvre.</p>
 *
 * <p>Il ne dépend volontairement <strong>pas</strong> de la migration {@code V003} : il crée le
 * rôle {@code OWNER} s'il manque. Deux raisons. D'abord l'ordre d'exécution entre le runner de
 * Mongock et celui-ci n'est pas garanti, et un garde-fou qui dépend d'un ordre non garanti n'en
 * est pas un. Ensuite {@code V003} ne tourne qu'une fois : si quelqu'un supprime le rôle en
 * base, seul ce runner le rétablit.</p>
 *
 * <p>Il ne touche jamais au rôle d'un autre compte, et ne retire rien : il pose, il n'arbitre pas.</p>
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
            // Pas une exception : un cœur qui refuse de démarrer emporte tout le reste avec lui,
            // y compris les serveurs de jeu déjà en route. Mais l'absence doit sauter aux yeux.
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

    /**
     * Le rôle {@code OWNER} porte toujours toutes les permissions, y compris celles ajoutées à
     * l'enum depuis le dernier démarrage. C'est ce qui évite qu'une nouvelle permission arrive
     * sans que personne ne la détienne.
     */
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
