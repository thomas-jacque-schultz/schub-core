package schultz.thomas.schub.core.business.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import schultz.thomas.schub.core.api.dto.RiotAccountDto;
import schultz.thomas.schub.core.business.model.RiotAccountState;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.data.repository.UserRepository;

import java.time.Instant;
import java.util.Optional;

/**
 * Le lien entre un compte Schub et un compte Riot — <strong>le chaînon qui manquait</strong>.
 *
 * <p>{@code User} portait {@code riotPuuid} et {@code riotGameName} depuis le lot A, mais rien ne
 * permettait de les renseigner : aucun membre d'équipe lié ne pouvait donc avoir la moindre
 * statistique. C'est ce lot qui ouvre la porte, et tout le reste du chantier D en dépend.</p>
 *
 * <h2>La propriété du compte n'est pas vérifiée — et c'est assumé</h2>
 *
 * <p><strong>Rien ici ne prouve que la personne possède le compte Riot qu'elle déclare.</strong>
 * Elle affirme « je suis {@code Pseudo#TAG} », le connecteur confirme seulement que ce Riot ID
 * <em>existe</em>, et on la croit. La vérification réelle s'appelle RSO (Riot Sign On) : c'est un
 * second parcours OAuth et une approbation Riot distincte de la clé d'API, donc hors périmètre de
 * cette version (plan §D, « à anticiper »).</p>
 *
 * <p>Dans une équipe de cinq personnes qui se connaissent, usurper le Riot ID d'un coéquipier ne
 * rapporte rien et se voit immédiatement. Ce qui ne serait pas acceptable, c'est de <em>laisser
 * croire</em> à une vérification : d'où ce paragraphe, et le fait que le refus ci-dessous parle
 * de « déjà lié » et jamais de « ce compte ne vous appartient pas ». Le jour où RSO arrive, c'est
 * cette classe qu'il remplace, et le modèle ne bouge pas — le {@code puuid} est déjà la clé.</p>
 *
 * <h2>Ce qu'il ne fait pas : toucher aux équipes</h2>
 *
 * <p>Lier son compte ne rattache personne à une place d'effectif. C'est
 * {@code POST /teams/claim} qui le fait, il existe depuis le lot D.4, et c'est au front de
 * l'appeler juste après une liaison réussie. Écrire dans les équipes depuis ici inverserait la
 * seule dépendance permise entre les deux domaines — {@code team} lit l'identité, jamais
 * l'inverse (plan §D.2) — et dupliquerait une revendication déjà écrite et testée.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiotAccountService {

    private final UserRepository userRepository;
    private final RiotIdResolver riotIdResolver;

    /** Ce que l'appelant a déclaré, et où en est la résolution. Aucune écriture, aucun appel. */
    public RiotAccountDto of(User actor) {
        return toDto(actor);
    }

    /**
     * Déclare — ou re-déclare — le Riot ID de l'appelant.
     *
     * <p><strong>Idempotente, et c'est ce qui sert de « réessayer ».</strong> Renvoyer le même
     * Riot ID relance la résolution du {@code puuid} : c'est exactement ce dont on a besoin quand
     * le connecteur était éteint à la première tentative, et ça évite une quatrième route dont le
     * seul objet aurait été de rejouer un appel.</p>
     *
     * <p>L'ordre compte : on résout <em>avant</em> de refuser les doublons, parce que le
     * {@code puuid} est ce sur quoi porte le vrai contrôle. Refuser d'abord sur le pseudo
     * reviendrait à juger sur la donnée faible alors que la forte est à un appel.</p>
     */
    public RiotAccountDto link(User actor, String riotIdSaisi) {
        RiotId riotId = RiotId.parse(riotIdSaisi);
        String puuid = riotIdResolver.resolvePuuid(riotId.gameName(), riotId.tagLine()).orElse(null);

        refuseSiRevendiqueAilleurs(actor, riotId, puuid);

        boolean memeDeclaration = riotId.equalsIgnoreCase(actor.getRiotGameName(), actor.getRiotTagLine());
        actor.setRiotGameName(riotId.gameName());
        actor.setRiotTagLine(riotId.tagLine());
        actor.setRiotPuuid(puuid);
        if (!memeDeclaration || actor.getRiotLinkedAt() == null) {
            actor.setRiotLinkedAt(Instant.now());
        }

        User enregistre = userRepository.save(actor);
        if (puuid == null) {
            log.warn("Riot ID {} déclaré par {} sans puuid — le connecteur Riot n'a pas répondu, "
                            + "la déclaration est conservée et sera résolue à la prochaine tentative",
                    riotId.riotId(), actor.getDiscordId());
        } else {
            log.info("Riot ID {} lié au compte {}", riotId.riotId(), actor.getDiscordId());
        }
        return toDto(enregistre);
    }

    /**
     * Retire le lien. Les trois champs partent ensemble : garder le pseudo sans le {@code puuid}
     * laisserait un compte à demi lié, dans un état qu'aucune route ne sait plus produire.
     *
     * <p><strong>Personne n'est retiré d'aucune équipe.</strong> Une place d'effectif revendiquée
     * l'est par {@code userId}, pas par {@code puuid} : elle survit et c'est voulu — on ne quitte
     * pas son équipe parce qu'on a délié son compte de jeu, on la quitte quand le capitaine vous
     * en retire. Écrire dans les équipes depuis ici serait de toute façon interdit.</p>
     */
    public RiotAccountDto unlink(User actor) {
        if (actor.getRiotPuuid() == null && actor.getRiotGameName() == null) {
            return toDto(actor);
        }
        log.info("Compte Riot délié de {}", actor.getDiscordId());
        actor.setRiotPuuid(null);
        actor.setRiotGameName(null);
        actor.setRiotTagLine(null);
        actor.setRiotLinkedAt(null);
        return toDto(userRepository.save(actor));
    }

    // --- règles ---

    /**
     * <strong>Deux comptes ne revendiquent pas le même joueur.</strong>
     *
     * <p>C'est le refus le plus important de ce lot : tout le reste du chantier D part du
     * {@code puuid}, et deux comptes qui portent le même donneraient des statistiques attribuées
     * à la mauvaise personne — une donnée fausse, jamais signalée, sur laquelle des panneaux
     * entiers se construiraient.</p>
     *
     * <p><strong>Le contrôle porte sur le {@code puuid} quand on l'a, sur le Riot ID sinon</strong>,
     * et la nuance n'est pas cosmétique :</p>
     *
     * <ul>
     *   <li><em>Résolu</em> : seul le {@code puuid} est comparé. Un Riot ID identique à celui
     *       enregistré sur un compte déjà résolu ne bloque rien, et il le faut — un joueur change
     *       de Riot ID quand il veut, son ancien pseudo reste écrit sur son compte, et il peut
     *       parfaitement être repris par quelqu'un d'autre. Bloquer là-dessus refuserait le vrai
     *       propriétaire au profit d'une chaîne périmée.</li>
     *   <li><em>Non résolu</em> : le Riot ID est tout ce qu'on a, et on ne le compare qu'aux
     *       comptes eux-mêmes non résolus. Comparer à un compte résolu reviendrait à laisser une
     *       chaîne périmée décider à la place de la clé stable.</li>
     * </ul>
     *
     * <p>Il reste donc un cas où deux comptes portent la même chaîne : l'un résolu, l'autre en
     * attente. Il se résorbe à la première résolution réussie du second, et il est sans danger —
     * {@code /teams/claim} ne rapproche par Riot ID que des places <em>libres</em>, et la
     * première revendication les rend liées. Aucun des deux ne peut prendre la place de l'autre.</p>
     */
    private void refuseSiRevendiqueAilleurs(User actor, RiotId riotId, String puuid) {
        if (puuid != null) {
            userRepository.findByRiotPuuid(puuid)
                    .filter(autre -> !autre.getId().equals(actor.getId()))
                    .ifPresent(autre -> {
                        throw new IllegalStateException(
                                "Ce compte Riot est déjà lié à un autre compte Schub");
                    });
            return;
        }
        userRepository.findByRiotGameNameIgnoreCaseAndRiotTagLineIgnoreCase(
                        riotId.gameName(), riotId.tagLine()).stream()
                .filter(autre -> !autre.getId().equals(actor.getId()))
                .filter(autre -> autre.getRiotPuuid() == null)
                .findFirst()
                .ifPresent(autre -> {
                    throw new IllegalStateException(
                            "Ce Riot ID est déjà déclaré par un autre compte Schub");
                });
    }

    private RiotAccountDto toDto(User user) {
        RiotId riotId = RiotId.deOuNull(user.getRiotGameName(), user.getRiotTagLine());
        if (riotId == null) {
            return new RiotAccountDto(RiotAccountState.ABSENT, null, null, null, null);
        }
        RiotAccountState state = user.getRiotPuuid() == null || user.getRiotPuuid().isBlank()
                ? RiotAccountState.EN_ATTENTE_DE_RESOLUTION
                : RiotAccountState.RESOLU;
        return new RiotAccountDto(
                state, riotId.riotId(), riotId.gameName(), riotId.tagLine(), user.getRiotLinkedAt());
    }

    /**
     * Un Riot ID découpé une bonne fois : {@code Pseudo#TAG} → {@code (gameName, tagLine)}.
     *
     * <p>Le découpage se fait sur le <strong>premier</strong> {@code #} et la partie droite ne
     * doit pas en contenir : un {@code gameName} ne peut pas porter de {@code #} chez Riot, donc
     * tout second {@code #} est une faute de saisie. La tolérer donnerait un {@code tagLine} qui
     * ne résoudra jamais, et un compte bloqué en attente de résolution sans qu'on sache pourquoi.</p>
     */
    record RiotId(String gameName, String tagLine) {

        private static final int LONGUEUR_MAX = 64;

        static RiotId parse(String saisie) {
            String propre = saisie == null ? "" : saisie.trim();
            if (propre.isEmpty()) {
                throw new IllegalArgumentException("Un Riot ID est obligatoire, sous la forme Pseudo#TAG");
            }
            if (propre.length() > LONGUEUR_MAX) {
                throw new IllegalArgumentException(
                        "Ce Riot ID dépasse " + LONGUEUR_MAX + " caractères — ce n'en est pas un");
            }
            int separateur = propre.indexOf('#');
            if (separateur < 0) {
                throw new IllegalArgumentException(
                        "Un Riot ID s'écrit Pseudo#TAG — le # et le tag sont obligatoires");
            }
            String gameName = propre.substring(0, separateur).trim();
            String tagLine = propre.substring(separateur + 1).trim();
            if (gameName.isEmpty() || tagLine.isEmpty() || tagLine.indexOf('#') >= 0) {
                throw new IllegalArgumentException(
                        "Un Riot ID s'écrit Pseudo#TAG, avec un seul # et rien de vide autour");
            }
            return new RiotId(gameName, tagLine);
        }

        static RiotId deOuNull(String gameName, String tagLine) {
            return gameName == null || gameName.isBlank() || tagLine == null || tagLine.isBlank()
                    ? null
                    : new RiotId(gameName.trim(), tagLine.trim());
        }

        boolean equalsIgnoreCase(String autreGameName, String autreTagLine) {
            return Optional.ofNullable(deOuNull(autreGameName, autreTagLine))
                    .filter(autre -> autre.gameName().equalsIgnoreCase(gameName)
                            && autre.tagLine().equalsIgnoreCase(tagLine))
                    .isPresent();
        }

        String riotId() {
            return gameName + "#" + tagLine;
        }
    }
}
