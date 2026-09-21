package schultz.thomas.schub.core.api.dto;

import java.time.Instant;
import java.util.List;

/**
 * Un compte Riot proposé à la personne qui cherche le sien.
 *
 * <p>Les propositions viennent de notre index des comptes connus : les joueurs croisés dans nos
 * parties, et ceux qu'une vérification a fait confirmer par Riot. L'API Riot, elle, ne sait pas
 * chercher par pseudo partiel.</p>
 *
 * <p><strong>Le {@code puuid} n'est pas rendu</strong>, comme dans {@link RiotAccountDto} : le
 * front n'en a pas l'usage. Cliquer une proposition revient à saisir son {@code riotId}, que la
 * route de liaison résout exactement — un seul chemin d'écriture, celui qui est déjà testé.</p>
 *
 * @param lastPlayedAt la dernière partie où <em>nous</em> l'avons croisé. Nulle pour un compte
 *                     confirmé par Riot et jamais rencontré : c'est zéro partie, pas une panne
 * @param observedAt   quand cette identité a été observée. C'est ce qui dit si le Riot ID affiché
 *                     est frais ou s'il mérite d'être revérifié — un Riot ID change de main
 * @param source       {@code PARTICIPATION} ou {@code RESOLUTION}, servi en chaîne. Le connecteur
 *                     reste libre d'en ajouter une sans que le front cesse de fonctionner
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
        Instant observedAt,
        String source,
        boolean alreadyLinked,
        boolean mine
) {

    public record PositionPlayedDto(String position, long matches) {
    }
}
