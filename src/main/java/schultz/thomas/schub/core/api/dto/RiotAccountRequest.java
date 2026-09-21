package schultz.thomas.schub.core.api.dto;

/**
 * Déclarer son Riot ID.
 *
 * <h2>Un seul champ pour le Riot ID, et pas deux</h2>
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
 * @param confirmChange remplacer un compte déjà lié par un <em>autre</em> compte est refusé en
 *               409 tant que ce drapeau n'est pas posé, et le corps du refus dit ce que le
 *               changement emporte. Ce n'est pas une politesse : le {@code puuid} est la clé de
 *               tout l'historique, et le changer en un clic serait une perte silencieuse. Il ne
 *               sert à rien pour une première déclaration ni pour une relance sur le même Riot ID
 */
public record RiotAccountRequest(String riotId, boolean confirmChange) {
}
