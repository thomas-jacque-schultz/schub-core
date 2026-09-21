package schultz.thomas.schub.core.team.api.dto;

/**
 * Un bilan de parties d'équipe sur une clé.
 *
 * @param key   le mode de jeu, le côté (100 ou 200) ou le patch, selon la coupe. Vide pour le
 *              total. Le mode est un nom de {@code QueueKind} et non un {@code queueId} : « File
 *              1700 » ne se lit pas.
 * @param label un libellé déjà rendu, quand la clé n'en porte pas. Nul partout aujourd'hui —
 *              le site est bilingue, une phrase servie ici n'existerait que dans une langue.
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
