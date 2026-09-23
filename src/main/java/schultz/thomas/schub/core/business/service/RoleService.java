package schultz.thomas.schub.core.business.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import schultz.thomas.schub.core.api.dto.RoleDto;
import schultz.thomas.schub.core.business.model.Permission;
import schultz.thomas.schub.core.business.model.SystemRole;
import schultz.thomas.schub.core.data.model.Role;
import schultz.thomas.schub.core.data.repository.RoleRepository;
import schultz.thomas.schub.core.data.repository.UserRepository;

import java.util.EnumSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    public List<Role> findAll() {
        return roleRepository.findAll();
    }

    public Role create(RoleDto candidate) {
        String name = requireName(candidate.name());
        if (roleRepository.findByName(name).isPresent()) {
            throw new IllegalStateException("Un rôle porte déjà le nom '" + name + "'");
        }
        Role role = new Role();
        role.setName(name);
        role.setPermissions(sanitize(candidate.permissions()));
        role.setSystem(false);
        log.info("Rôle '{}' créé avec {}", name, role.getPermissions());
        return roleRepository.save(role);
    }

    public Role update(String id, RoleDto candidate) {
        Role role = require(id);
        if (SystemRole.OWNER.roleName().equals(role.getName())) {
            throw new IllegalStateException(
                    "Le rôle " + SystemRole.OWNER.roleName() + " ne se modifie pas : c'est le garde-fou anti-verrouillage");
        }
        if (!role.isSystem()) {
            String name = requireName(candidate.name());
            if (!name.equals(role.getName()) && roleRepository.findByName(name).isPresent()) {
                throw new IllegalStateException("Un rôle porte déjà le nom '" + name + "'");
            }
            role.setName(name);
        }
        role.setPermissions(sanitize(candidate.permissions()));
        log.info("Rôle '{}' mis à jour avec {}", role.getName(), role.getPermissions());
        return roleRepository.save(role);
    }

    public void delete(String id) {
        Role role = require(id);
        if (role.isSystem()) {
            throw new IllegalStateException("Un rôle système ne se supprime pas : " + role.getName());
        }
        long holders = userRepository.countByRoleId(id);
        if (holders > 0) {
            throw new IllegalStateException(
                    holders + " compte(s) portent le rôle '" + role.getName() + "' — réattribuez-les d'abord");
        }
        roleRepository.deleteById(id);
        log.info("Rôle '{}' supprimé", role.getName());
    }

    public RoleDto toDto(Role role) {
        return new RoleDto(role.getId(), role.getName(), role.getPermissions(), role.isSystem());
    }

    private Role require(String id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Aucun rôle d'identifiant '" + id + "'"));
    }

    private String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Un rôle a besoin d'un nom");
        }
        return name.trim();
    }

    // ROLE_MANAGE retirée sans refus : la recevoir signale un appel forgé, refuser en révélerait l'existence.
    private Set<Permission> sanitize(Set<Permission> permissions) {
        Set<Permission> sanitized = permissions == null || permissions.isEmpty()
                ? EnumSet.noneOf(Permission.class)
                : EnumSet.copyOf(permissions);
        if (sanitized.remove(Permission.ROLE_MANAGE)) {
            log.warn("ROLE_MANAGE retirée d'un rôle : elle n'est pas attribuable (décision n°2 du 18-09)");
        }
        return sanitized;
    }
}
