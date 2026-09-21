package schultz.thomas.schub.core.team.business.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Ce que le pool de champions demande au connecteur Riot : des maîtrises, et un catalogue.
 *
 * <h2>Aucun cache de ce côté-ci, et ce n'est pas une omission</h2>
 *
 * <p>Le connecteur détient déjà les quatre politiques du plan §D.2 ter — maîtrises à six heures,
 * catalogue permanent par version — et il a une base pour ça. Un second cache dans le cœur
 * donnerait deux vérités et une divergence garantie : le jour où l'une est rafraîchie et l'autre
 * non, personne ne saurait laquelle est affichée. Ce qui est en face est un appel local sur
 * l'overlay, il n'y a rien à économiser.</p>
 *
 * <p>Conséquence assumée : ouvrir le panneau appelle le connecteur une fois pour le catalogue et
 * une fois par membre pourvu d'un {@code puuid}. À cinq joueurs, six requêtes locales dont le
 * connecteur sert la quasi-totalité depuis sa base.</p>
 *
 * <h2>Pourquoi des {@link Optional}, et où ils ne sont pas</h2>
 *
 * <p>{@code Optional<List<…>>} peut ressembler à une coquetterie ; c'est la seule forme qui
 * distingue <strong>« je n'ai pas pu demander »</strong> de <strong>« la réponse est vide »</strong>.
 * Une liste vide pour les deux ferait afficher « aucun champion joué » à un membre dont on n'a
 * simplement pas su lire les maîtrises — un mensonge silencieux, et exactement le genre de chose
 * que ce panneau doit dire au lieu de le taire.</p>
 */
public interface RiotChampionGateway {

    /**
     * Le catalogue des champions à la version courante, ou rien si le connecteur n'a pas répondu.
     *
     * <p>C'est lui qui porte la <strong>version du patch</strong>. Sans elle, une icône ou un nom
     * de champion relu dans trois mois est faux sans qu'on le voie — d'où la règle du service
     * appelant : pas de catalogue, pas un seul champion rendu.</p>
     */
    Optional<Catalogue> catalogue();

    /**
     * Les {@code limit} champions les plus maîtrisés de ce joueur.
     *
     * @return {@link Optional#empty()} si le connecteur n'a pas répondu ou ne connaît pas ce
     *         {@code puuid} ; une liste vide s'il répond qu'il n'y a aucune maîtrise
     */
    Optional<List<Mastery>> masteries(String puuid, int limit);

    /**
     * Toutes les maîtrises de ce joueur.
     *
     * <p>Le pool croise un choix de champions fait par l'équipe : un « top 10 » répondrait
     * « aucune maîtrise » sur le onzième champion retenu, ce qui est faux et invisible.</p>
     */
    default Optional<List<Mastery>> masteries(String puuid) {
        return masteries(puuid, 0);
    }

    /**
     * Le catalogue figé à une version.
     *
     * @param parId indexé par l'identifiant numérique, parce que c'est lui que portent les
     *              maîtrises — le croisement se fait là-dessus, jamais sur un nom
     */
    record Catalogue(String version, Map<Integer, Champion> parId) {
    }

    /**
     * @param key l'identifiant textuel Data Dragon ({@code MonkeyKing}), qui n'est pas toujours
     *            le nom affiché ({@code Wukong})
     */
    record Champion(int id, String key, String name, String iconUrl) {
    }

    /**
     * @param observedAt la date du relevé, telle que le connecteur la donne. Servie jusqu'à
     *                   l'écran : c'est elle qui dit l'âge de ce qu'on affiche, et le cache de
     *                   six heures la rend rarement immédiate
     */
    record Mastery(int championId, int level, int points, Instant lastPlayedAt, Instant observedAt) {
    }
}
