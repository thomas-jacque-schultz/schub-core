package schultz.thomas.schub.core.team.data.model;

import lombok.Data;
import schultz.thomas.schub.core.team.business.model.GameRole;
import schultz.thomas.schub.core.team.business.model.MemberStatus;

import java.time.Instant;

/**
 * Quelqu'un dans l'effectif d'une équipe — <strong>lié à un compte Schub, ou libre</strong>.
 *
 * <p>C'est la décision qui commande tout le reste (plan §D.2 bis, point 1) : si un membre devait
 * avoir un compte, aucune équipe n'existerait tant que les cinq ne se sont pas connectés avec
 * Discord, et on ne pourrait rien préparer tout seul. Un membre <em>libre</em> n'est identifié
 * que par son Riot ID ; il devient <em>lié</em> le jour où la personne se connecte et revendique
 * ce Riot ID — c'est à cet instant que l'équipe apparaît « sur son compte ».</p>
 *
 * <p><strong>{@code userId} est un id, et rien qu'un id.</strong> Pas de {@code @DBRef}, pas de
 * pseudo recopié : un pseudo recopié serait faux le jour où il change, et une jointure Mongo
 * serait exactement ce qui rendrait l'extraction du domaine coûteuse. Le pseudo à afficher est
 * résolu à la projection, par le seul point de contact prévu pour ça.</p>
 *
 * <p><strong>{@code riotPuuid} peut être nul, {@code riotGameName}/{@code riotTagLine} non.</strong>
 * Le {@code puuid} est la seule clé stable côté Riot — un joueur change de Riot ID quand il veut
 * — mais le résoudre demande le connecteur, qui peut être indisponible. Faire dépendre la
 * création d'une équipe de la disponibilité de l'API Riot serait un mauvais échange : on accepte
 * le membre sans {@code puuid} et on le complète plus tard.</p>
 */
@Data
public class TeamMember {

    /**
     * L'identifiant de ce membre <em>dans son équipe</em>, stable, tiré au sort à l'ajout.
     *
     * <p>Il existe parce qu'un membre libre n'a ni {@code userId} ni forcément {@code puuid} :
     * sans lui, une composition ne pourrait désigner personne, et retirer un membre demanderait
     * de le nommer. Un index de position aurait bougé au premier retrait.</p>
     */
    private String memberId;

    /** L'id interne du compte Schub, ou {@code null} tant que le membre est libre. */
    private String userId;

    private String riotPuuid;
    private String riotGameName;
    private String riotTagLine;

    /** Le poste. {@code null} est licite : un coach n'en a pas. */
    private GameRole role;

    private MemberStatus status;

    private Instant addedAt;

    /** Depuis quand ce membre est lié à un compte. {@code null} s'il est encore libre. */
    private Instant linkedAt;

    /** Un fait dérivé, jamais persisté : le stocker créerait une seconde vérité. */
    public boolean isLinked() {
        return userId != null && !userId.isBlank();
    }

    /** {@code Pseudo#TAG}, ou {@code null} si ce membre n'a pas de Riot ID connu. */
    public String riotId() {
        if (riotGameName == null || riotGameName.isBlank()) {
            return null;
        }
        return riotTagLine == null || riotTagLine.isBlank()
                ? riotGameName
                : riotGameName + "#" + riotTagLine;
    }
}
