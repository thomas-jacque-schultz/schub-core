package schultz.thomas.schub.core.team.api.dto;

/**
 * Un bilan de parties d'équipe sur une clé.
 *
 * @param key   {@code queueId}, côté (100 ou 200) ou patch, selon la coupe. Vide pour le total.
 * @param label le nom que le connecteur donne à la file, quand la clé en a un.
 */
public record TeamRecordDto(
        String key,
        String label,
        long games,
        long wins,
        long losses,
        Double winRate,
        Double averageDurationSeconds
) {
}
