package schultz.thomas.schub.core.business.model;

/**
 * Un verbe atomique, figé dans le code.
 *
 * <p><strong>Une enum, pas des lignes en base — et c'est la décision, pas un raccourci.</strong>
 * Une permission ne vaut que par le code qui la vérifie : en créer une depuis une interface
 * produirait un droit que rien ne lit, donc une promesse fausse. Les <em>rôles</em>, eux, vivent
 * en base et sont éditables — c'est là qu'est la souplesse demandée (plan §A.1).</p>
 *
 * <p>Deux sources d'autorité cohabitent, et c'est voulu : les permissions du rôle donnent un
 * droit <em>partout</em> ; appartenir aux {@code admins} d'un serveur donne
 * {@link #SERVER_START} et {@link #SERVER_STOP} sur <em>ce</em> serveur, rien d'autre
 * (décision n°11 du 18-09). Modifier des ports, c'est écrire dans la table de redirections de
 * la box : un pouvoir global qu'on ne déguise pas en pouvoir local.</p>
 */
public enum Permission {

    /** La vue « membre » d'un serveur : URL de connexion, version, description, historique. */
    SERVER_VIEW,

    /**
     * Ports, déploiement, administrateurs : tout ce qui décrit l'infrastructure.
     *
     * <p>Séparée de {@link #SERVER_VIEW} parce qu'un {@code VISITEUR} n'est pas un proche :
     * c'est n'importe qui sur Internet muni d'un compte Discord. Lui servir {@code deploymentId}
     * et la liste des ports ouverts serait offrir une cartographie gratuite de la maison.</p>
     */
    SERVER_INFRA_VIEW,

    SERVER_START,
    SERVER_STOP,

    SERVER_CREATE,
    SERVER_EDIT,
    SERVER_DELETE,

    PORT_VIEW,
    PORT_RULE_EDIT,

    DISCORD_CHANNEL_MANAGE,

    USER_VIEW,
    USER_ROLE_ASSIGN,

    // --- domaine d'équipe (chantier D, lot D.4) ---
    //
    // Les trois dernières s'évaluent SUR une équipe : en être membre y donne des droits,
    // exactement comme figurer dans les `admins` d'un serveur en donne sur celui-là. La
    // première est globale — créer n'a pas de ressource sur laquelle porter.

    /**
     * Créer une équipe. <strong>Globale, et donnée au rôle {@code VISITEUR}</strong> : sans
     * elle, personne ne peut rien faire de l'outil, ce qui viderait le chantier de son sens
     * (plan §D.2 bis).
     */
    TEAM_CREATE,

    /** Voir une équipe, son effectif et ses compositions. En être membre suffit. */
    TEAM_VIEW,

    /** Renommer une équipe, ajouter, modifier ou retirer un membre. Son créateur, et {@code OWNER}. */
    TEAM_EDIT,

    /**
     * Écrire les compositions d'une équipe.
     *
     * <p>Séparée de {@link #TEAM_EDIT} bien qu'elles aillent aujourd'hui aux mêmes personnes :
     * préparer une composition et modifier l'effectif ne sont pas le même geste. Le jour où un
     * membre devra pouvoir proposer une composition sans toucher au roster, la séparation
     * existe déjà — l'ajouter après coup obligerait à relire chaque appelant.</p>
     */
    COMPOSITION_EDIT,

    /**
     * L'écran des rôles — la fenêtre réservée.
     *
     * <p><strong>Jamais attribuable</strong> (décision n°2 du 18-09) : ni via l'API des rôles,
     * ni indirectement. Elle n'appartient qu'au rôle système {@code OWNER}, ce qui rend
     * « accessible seulement à moi-même » vrai par construction et non par convention.</p>
     */
    ROLE_MANAGE
}
