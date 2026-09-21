package schultz.thomas.schub.core.api.dto;

import java.time.Instant;
import java.util.List;

/**
 * Un compte Riot proposé à la personne qui cherche le sien.
 *
 * <p>Les suggestions viennent de <strong>nos</strong> parties collectées, pas de Riot : l'API
 * Riot ne sait pas chercher par pseudo partiel. Ce sont donc des comptes qu'on a croisés, et les
 * chiffres servent à reconnaître le sien parmi des homonymes.</p>
 *
 * <p><strong>Le {@code puuid} n'est pas rendu</strong>, comme dans {@link RiotAccountDto} : le
 * front n'en a pas l'usage. Cliquer une proposition revient à saisir son {@code riotId}, que la
 * route de liaison résout exactement — un seul chemin d'écriture, celui qui est déjà testé.</p>
 *
 * @param alreadyLinked ce compte est déjà revendiqué par un <em>autre</em> compte Schub. C'est le
 *                      « pourquoi » que le connecteur ne peut pas connaître, et le poser ici
 *                      évite de proposer un choix qui sera refusé en 409 au clic suivant
 * @param mine          ce compte est déjà le vôtre — la proposition reste affichée plutôt que
 *                      retirée, sinon on croit l'avoir mal saisi
 */
public record RiotAccountSuggestionDto(
        String riotId,
        String gameName,
        String tagLine,
        long matchCount,
        List<PositionPlayedDto> positions,
        Instant lastPlayedAt,
        boolean alreadyLinked,
        boolean mine
) {

    public record PositionPlayedDto(String position, long matches) {
    }
}
