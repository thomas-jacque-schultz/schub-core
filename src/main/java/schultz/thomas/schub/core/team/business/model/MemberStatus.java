package schultz.thomas.schub.core.team.business.model;

/**
 * La place d'un membre dans l'effectif.
 *
 * <p>Elle existe parce qu'il a été décidé de <strong>ne pas limiter une équipe à cinq
 * joueurs</strong> : un roster réel a des remplaçants et un coach, et coder « une équipe = 5 »
 * en dur est le genre d'hypothèse qui se paie six mois plus tard (plan §D.2 bis, point 3).
 * Sans ce statut, une équipe de sept est un tas indistinct.</p>
 *
 * <p><strong>À ne pas confondre avec lié / libre</strong>, qui n'est pas un statut mais un fait :
 * un membre est lié quand {@code userId} est renseigné, libre sinon. Le dupliquer en champ
 * donnerait deux vérités qui finiraient par diverger.</p>
 */
public enum MemberStatus {

    /** Dans le cinq de départ. */
    TITULAIRE,

    /** De l'effectif, pas du cinq de départ. */
    REMPLACANT,

    /** De l'équipe sans y jouer : aucun poste, et jamais retenu dans une composition. */
    COACH
}
