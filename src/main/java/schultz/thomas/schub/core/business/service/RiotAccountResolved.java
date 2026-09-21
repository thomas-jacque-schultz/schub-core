package schultz.thomas.schub.core.business.service;

/**
 * Publié quand un lien Riot obtient enfin son puuid. Autoporteur : l'identité ne connaît pas les
 * domaines qui écoutent, et aucun d'eux n'a besoin de relire l'utilisateur. Le jour où l'un d'eux
 * sort du cœur, l'écoute devient un appel sans rien changer ici.
 */
public record RiotAccountResolved(String userId, String discordId, String puuid,
                                  String gameName, String tagLine) {
}
