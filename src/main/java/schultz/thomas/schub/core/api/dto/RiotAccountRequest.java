package schultz.thomas.schub.core.api.dto;

/**
 * Déclarer son Riot ID.
 *
 * <h2>Un seul champ, et pas deux</h2>
 *
 * <p>{@code AddMemberRequest} sépare {@code riotGameName} et {@code riotTagLine} parce qu'un
 * capitaine remplit deux cases pour quelqu'un d'autre. Ici, la personne recopie ce que le client
 * de jeu lui affiche : {@code Pseudo#TAG}, d'un bloc. Offrir les deux formes reviendrait à avoir
 * deux façons de dire la même chose, donc deux chemins à tester et un désaccord possible entre
 * eux — le découpage se fait une fois, dans le domaine, et il est le même pour tout le monde.</p>
 *
 * @param riotId {@code Pseudo#TAG}. Exactement un {@code #}, et rien de vide de part et d'autre ;
 *               toute autre forme est refusée en 400 plutôt qu'enregistrée telle quelle. Le
 *               {@code tagLine} s'écrit sans le {@code #} côté Riot, mais personne ne le saisit
 *               comme ça — c'est au serveur de le savoir, pas à l'utilisateur
 */
public record RiotAccountRequest(String riotId) {
}
