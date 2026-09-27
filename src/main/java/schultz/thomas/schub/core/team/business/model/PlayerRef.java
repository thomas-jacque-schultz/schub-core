package schultz.thomas.schub.core.team.business.model;

// Un joueur dont on lit les parties : le compte connecté, ou un joueur recherché par son Riot ID.
public record PlayerRef(String id, String displayName, String puuid, String gameName, String tagLine) {
}
