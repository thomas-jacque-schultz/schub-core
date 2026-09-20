package schultz.thomas.schub.core.team.api.dto;

import java.time.Instant;
import java.util.List;

/**
 * Une composition préparée : cinq lignes, le patch qui leur donne leur sens, des notes.
 *
 * <p>{@code patch} n'est pas décoratif : les données de champions dépendent de la version de Data
 * Dragon, et une composition relue dans trois mois sans sa version afficherait des champions qui
 * n'existaient pas quand elle a été écrite.</p>
 */
public record CompositionDto(
        String id,
        String teamId,
        String name,
        List<CompositionSlotDto> slots,
        String patch,
        String notes,
        Instant createdAt,
        Instant updatedAt,
        boolean viewerCanEdit
) {
}
