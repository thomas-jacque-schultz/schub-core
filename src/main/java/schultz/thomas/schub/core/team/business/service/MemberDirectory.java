package schultz.thomas.schub.core.team.business.service;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * <strong>Le seul point de contact entre ce domaine et celui de l'identité.</strong>
 *
 * <p>Le plan §D.2 pose que le lien avec l'identité est un id, et rien qu'un id. Mais afficher une
 * équipe demande des pseudos, et ajouter un membre demande de savoir si ce Riot ID appartient
 * déjà à un compte : il faut donc <em>quelque part</em> une lecture de l'identité. Toute la
 * question est qu'elle soit à un seul endroit et qu'elle ressemble déjà à ce qu'elle deviendra.
 * C'est cette interface — le jour où le domaine d'équipe devient un service, son implémentation
 * devient un client HTTP vers {@code /users}, et pas une ligne du domaine ne bouge.</p>
 *
 * <p>Ce qui reste interdit, et que cette interface ne permet pas : joindre un {@code User} dans
 * une requête Mongo, ou recopier un pseudo dans un document d'équipe. Un pseudo recopié est faux
 * le jour où il change, et une jointure est ce qui rendrait l'extraction coûteuse.</p>
 */
public interface MemberDirectory {

    /**
     * Ce qu'on a le droit de savoir d'un compte pour l'afficher dans une équipe : de quoi le
     * reconnaître, rien de plus. Ni son rôle, ni ses droits, ni son identifiant Discord.
     */
    record MemberIdentity(
            String userId,
            String displayName,
            String avatarUrl,
            String riotPuuid,
            String riotGameName,
            String riotTagLine
    ) {
    }

    /**
     * Les comptes désignés par leurs ids internes, indexés — <strong>un seul aller-retour pour
     * toute la page</strong>, et non un par membre : c'est exactement ce que le front ne doit
     * pas avoir à faire.
     */
    Map<String, MemberIdentity> byIds(Set<String> userIds);

    /**
     * Le compte qui a revendiqué ce {@code puuid}, s'il existe.
     *
     * <p>Sert à lier un membre dès son ajout quand la personne a déjà un compte Schub : sans
     * cela, elle devrait se reconnecter pour voir apparaître une équipe qu'on vient de créer
     * pour elle.</p>
     */
    Optional<MemberIdentity> byRiotPuuid(String riotPuuid);
}
