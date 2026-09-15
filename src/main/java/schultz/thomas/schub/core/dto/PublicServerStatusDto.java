package schultz.thomas.schub.core.dto;

/** Vue publique, sans rien qui révèle l'infrastructure : ni déploiement, ni ports, ni admins. */
public record PublicServerStatusDto(
        String name,
        String game,
        String status
) {
}
