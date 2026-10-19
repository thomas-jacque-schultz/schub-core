package schultz.thomas.schub.core.lol.stats.api.dto;

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
