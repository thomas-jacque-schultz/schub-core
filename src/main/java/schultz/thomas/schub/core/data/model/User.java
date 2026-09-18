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

    private Instant createdAt;
    private Instant lastLoginAt;
}
