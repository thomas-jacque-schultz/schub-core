package schultz.thomas.schub.core.data.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Un compte du système. Depuis le 18-09, il vit dans le cœur et non dans le connecteur Discord.
 *
 * <p>Le raisonnement tient en une ligne : un rôle nommé « modérateur » qui contient
 * « démarrer un serveur », ce n'est pas de l'état de plateforme Discord, c'est du domaine. Le
 * connecteur garde ce qui est vraiment de Discord — salons, messages, guildes (plan §1).</p>
 *
 * <p><strong>{@code discordId} est un identifiant externe.</strong> Il sert à reconnaître qui se
 * connecte, jamais à désigner l'utilisateur ailleurs dans le domaine : les {@code admins} d'un
 * serveur portent l'{@code id} interne, pour qu'un compte lié autrement un jour ne rende pas
 * les listes fausses (plan §A.4).</p>
 */
@Data
@Document(collection = "users")
public class User {

    @Id
    private String id;

    /** Identifiant Discord, unique. C'est par lui que le BFF retrouve le compte à la connexion. */
    @Indexed(unique = true)
    private String discordId;

    private String discordUsername;
    private String avatarUrl;

    /**
     * Le nom affiché sur le site, choisi par la personne.
     *
     * <p><strong>{@code null} tant qu'elle n'en a pas choisi un</strong>, et c'est ce qui fait
     * que le pseudo Discord continue de suivre ses changements. Recopier le pseudo Discord à la
     * création figerait un nom que plus rien ne rafraîchirait.</p>
     *
     * <p>Il n'est <strong>pas unique</strong> : ce n'est pas un identifiant, rien ne s'y connecte
     * et rien ne s'y retrouve. L'unicité coûterait un refus incompréhensible sur un champ
     * décoratif, une course à la réservation de pseudo et un index à maintenir ; quand deux
     * personnes doivent être distinguées, c'est le pseudo Discord et l'avatar qui le font.
     * {@code discordId} reste la seule clé d'identité.</p>
     */
    private String displayName;

    /** Référence vers {@link Role}. Un utilisateur a exactement un rôle. */
    private String roleId;

    // --- lien vers le compte Riot, préparé pour le chantier D ---
    //
    // Il vit sur le User et non dans le futur paquet `team` : l'identité reste une, et un
    // service d'équipe extrait plus tard ne reprendrait pas la propriété des comptes (plan §D.2).
    // Le puuid est la seule clé stable côté Riot — un joueur change de Riot ID quand il veut.

    private String riotPuuid;
    private String riotGameName;
    private String riotTagLine;

    /**
     * Depuis quand ce Riot ID est déclaré — {@code null} tant qu'il ne l'est pas.
     *
     * <p>C'est la date de la <em>déclaration</em>, pas celle de la dernière résolution : relancer
     * la résolution du même Riot ID ne la bouge pas. Sans elle, un compte resté en attente de
     * résolution ne se distingue pas d'un compte lié hier, et on ne sait pas lequel relancer.</p>
     */
    private Instant riotLinkedAt;

    private Instant createdAt;
    private Instant lastLoginAt;
}
