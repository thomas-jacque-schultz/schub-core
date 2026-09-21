package schultz.thomas.schub.core.api.dto;

import schultz.thomas.schub.core.business.model.RiotAccountState;

import java.time.Instant;

/**
 * Le lien vers le compte Riot de <strong>l'appelant</strong>, tel qu'il est servi sur le fil.
 *
 * <h2>Ce qui n'y figure pas : le {@code puuid}</h2>
 *
 * <p>Il est pourtant la seule chose qui compte côté serveur. Mais aucun écran n'en a l'usage — il
 * ne se lit pas, ne s'affiche pas, ne se recopie pas — et c'est l'identifiant qui ouvre tout
 * l'historique de jeu d'une personne chez Riot. {@link #state} dit s'il est connu, ce qui est la
 * seule question que le front se pose.</p>
 *
 * <h2>Pourquoi aucun booléen {@code viewerCan…}</h2>
 *
 * <p>Le §A.5 bis du plan demande que « ai-je le droit ? » soit un fait sur le lecteur. Ici la
 * question ne se pose pas : la ressource <em>est</em> le lecteur. Les routes sont sous
 * {@code /users/me}, il n'existe aucun chemin vers le lien de quelqu'un d'autre, et un booléen
 * constamment vrai ne serait qu'un champ de plus à maintenir.</p>
 *
 * @param riotId   {@code Pseudo#TAG} reconstitué, ou {@code null} si rien n'est déclaré. Servi en
 *                 plus des deux parties parce que c'est sous cette forme que la personne l'a
 *                 saisi et qu'elle le relira
 * @param linkedAt depuis quand ce Riot ID est déclaré. Il ne bouge pas quand la résolution est
 *                 relancée sur le même Riot ID : c'est la date de la déclaration, pas celle du
 *                 dernier appel au connecteur
 */
public record RiotAccountDto(
        RiotAccountState state,
        String riotId,
        String gameName,
        String tagLine,
        Instant linkedAt
) {
}
