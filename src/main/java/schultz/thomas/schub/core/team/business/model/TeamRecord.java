package schultz.thomas.schub.core.team.business.model;

public record TeamRecord(
        String key,
        String label,
        long games,
        long wins,
        long losses,
        Double winRate,
        Double averageDurationSeconds
) {
}
