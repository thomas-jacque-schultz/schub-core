package schultz.thomas.schub.core.team.business.model;

/**
 * Les cinq postes d'une équipe de League of Legends.
 *
 * <p>Figés dans le code, comme {@link schultz.thomas.schub.core.business.model.Permission} : ce
 * sont les postes du jeu, pas une préférence d'organisation. Les rendre configurables donnerait
 * une liste qu'aucun écran ne saurait dessiner.</p>
 *
 * <p>Un membre peut n'en porter aucun — un coach n'a pas de poste, et un remplaçant polyvalent
 * non plus. Une <em>composition</em>, elle, les couvre tous les cinq, exactement une fois
 * chacun.</p>
 */
public enum GameRole {

    TOP,
    JGL,
    MID,
    ADC,
    SUP
}
