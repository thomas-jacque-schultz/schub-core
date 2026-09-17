package schultz.thomas.schub.core.data.model;

/** État d'un GameServer, tel que le cœur l'interprète depuis ce que rapporte le déploiement. */
public enum GameServerStatus {
    ONLINE,
    OFFLINE,
    /** Le déploiement n'a pas pu être consulté, ou sa dernière lecture est trop ancienne pour être crue. */
    UNREACHABLE,
    UNKNOWN
}
