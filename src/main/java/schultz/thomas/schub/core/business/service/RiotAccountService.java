package schultz.thomas.schub.core.business.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import schultz.thomas.schub.core.api.dto.RiotAccountChangeDto;
import schultz.thomas.schub.core.api.dto.RiotAccountDto;
import schultz.thomas.schub.core.api.dto.RiotAccountSuggestionDto;
import schultz.thomas.schub.core.api.dto.RiotIngestDto;
import schultz.thomas.schub.core.business.model.RiotAccountState;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.data.repository.UserRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Le lien entre un compte Schub et un compte Riot.
 *
 * <h2>La propriété du compte n'est pas vérifiée — et c'est assumé</h2>
 *
 * <p><strong>Rien ici ne prouve que la personne possède le compte Riot qu'elle déclare.</strong>
 * Elle affirme « je suis {@code Pseudo#TAG} », le connecteur confirme seulement que ce Riot ID
 * <em>existe</em>, et on la croit. La vérification réelle s'appelle RSO (Riot Sign On) : c'est un
 * second parcours OAuth et une approbation Riot distincte de la clé d'API, donc hors périmètre de
 * cette version (plan §D, « à anticiper »).</p>
 *
 * <h2>On change de compte, on ne délie pas</h2>
 *
 * <p>La route de déliaison a été retirée : un compte délié laisse les équipes dans un état
 * incohérent — des places d'effectif rattachées à quelqu'un qui n'a plus de compte de jeu, des
 * panneaux qui se vident sans raison lisible. Le geste utile est le <em>remplacement</em>, et il
 * est explicite : voir {@link #link}.</p>
 *
 * <h2>Ce qu'il ne fait pas : toucher aux équipes</h2>
 *
 * <p>Lier ou changer son compte ne rattache personne à une place d'effectif. C'est
 * {@code POST /teams/claim} qui le fait, et c'est au front de l'appeler juste après. Écrire dans
 * les équipes depuis ici inverserait la seule dépendance permise entre les deux domaines —
 * {@code team} lit l'identité, jamais l'inverse (plan §D.2).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiotAccountService {

    /** Mesuré : Riot ne garde qu'environ mille parties par joueur. */
    private static final int PARTIES_ESTIMEES = 1_000;

    /** Un appel par partie, au débit d'une clé aux limites de développement (50/min). */
    private static final Duration DUREE_ESTIMEE = Duration.ofMinutes(20);

    private final UserRepository userRepository;
    private final RiotIdResolver riotIdResolver;
    private final RiotConnectorService riotConnectorService;

    /** Ce que l'appelant a déclaré, où en est la résolution, et où en est la collecte. */
    public RiotAccountDto of(User actor) {
        return toDto(actor).withIngest(ingestOf(actor));
    }

    /**
     * Déclare, relance, ou <strong>remplace</strong> le compte Riot de l'appelant.
     *
     * <p>Trois gestes derrière une seule route, parce qu'ils ne se distinguent que par l'état
     * d'arrivée et qu'une route par geste obligerait le front à savoir lequel il fait :</p>
     *
     * <ul>
     *   <li><em>déclarer</em> — rien n'était lié. Direct.</li>
     *   <li><em>relancer</em> — même Riot ID resoumis. C'est le « réessayer » d'un écran dont la
     *       première tentative est tombée sur un connecteur éteint, et c'est aussi ce qui
     *       rattrape un {@code puuid} jamais résolu. Direct — et un connecteur muet ne fait rien
     *       perdre, le {@code puuid} déjà connu est conservé.</li>
     *   <li><em>remplacer</em> — un autre compte. <strong>Refusé en 409 tant que
     *       {@code confirmChange} n'est pas posé</strong>, avec les conséquences dans le corps.</li>
     * </ul>
     *
     * <p><strong>Changer de Riot ID n'est pas changer de compte.</strong> Un joueur renomme son
     * Riot ID quand il veut ; si le {@code puuid} résolu est le même qu'avant, c'est le même
     * compte et rien n'est perdu — aucune confirmation n'est demandée. Confondre les deux ferait
     * apparaître un avertissement effrayant sur une simple mise à jour de pseudo.</p>
     *
     * <p>La confirmation n'est exigée que si l'ancien lien était <em>résolu</em> : sans
     * {@code puuid}, rien n'a jamais été collecté et il n'y a rien à perdre — corriger une faute
     * de frappe avant résolution n'a pas à être confirmé.</p>
     */
    public RiotAccountDto link(User actor, String riotIdSaisi, boolean confirmChange) {
        RiotId riotId = RiotId.parse(riotIdSaisi);
        RiotIdResolution resolution = riotIdResolver.resolve(riotId.gameName(), riotId.tagLine());
        if (resolution.isNotFound()) {
            throw new UnknownRiotAccountException(riotId.gameName() + "#" + riotId.tagLine());
        }
        String puuid = resolution.puuid();

        refuseSiRevendiqueAilleurs(actor, riotId, puuid);

        String ancienPuuid = trimOrNull(actor.getRiotPuuid());
        RiotId ancienRiotId = RiotId.deOuNull(actor.getRiotGameName(), actor.getRiotTagLine());
        boolean memeDeclaration = riotId.equalsIgnoreCase(actor.getRiotGameName(), actor.getRiotTagLine());
        boolean memeCompte = ancienPuuid != null && ancienPuuid.equals(puuid);
        boolean remplacement = ancienPuuid != null && !memeDeclaration && !memeCompte;

        if (remplacement && !confirmChange) {
            throw new RiotAccountChangeNotConfirmedException(
                    changement(ancienRiotId, riotId, false));
        }

        actor.setRiotGameName(riotId.gameName());
        actor.setRiotTagLine(riotId.tagLine());
        actor.setRiotPuuid(puuid != null ? puuid : (memeDeclaration ? ancienPuuid : null));
        if (!memeDeclaration || actor.getRiotLinkedAt() == null) {
            actor.setRiotLinkedAt(Instant.now());
        }

        User enregistre = userRepository.save(actor);
        boolean collecteDemandee = demandeLaCollecte(enregistre, ancienPuuid, puuid);

        if (puuid == null) {
            log.warn("Riot ID {} déclaré par {} sans puuid — le connecteur Riot n'a pas répondu, "
                            + "la déclaration est conservée et sera résolue à la prochaine tentative",
                    riotId.riotId(), actor.getDiscordId());
        } else if (remplacement) {
            log.info("Compte Riot de {} remplacé : {} devient {}",
                    actor.getDiscordId(), ancienRiotId == null ? "(inconnu)" : ancienRiotId.riotId(),
                    riotId.riotId());
        } else {
            log.info("Riot ID {} lié au compte {}", riotId.riotId(), actor.getDiscordId());
        }

        RiotAccountDto dto = toDto(enregistre).withIngest(ingestOf(enregistre));
        return remplacement ? dto.withChange(changement(ancienRiotId, riotId, collecteDemandee)) : dto;
    }

    /**
     * Les comptes connus qui ressemblent à cette saisie — et, si la saisie <em>est</em> un Riot ID
     * complet, ce que Riot en dit avant de répondre.
     *
     * <h2>Une saisie partielle cherche, un Riot ID complet vérifie</h2>
     *
     * <p>Chercher {@code Thom} est une question sur nos données : l'index y répond seul, sans un
     * appel sortant. Écrire {@code Thomas#EUW} est autre chose — c'est nommer un compte précis, et
     * la seule autorité sur son existence est Riot. Le cœur le fait donc résoudre d'abord ; la
     * résolution inscrit l'observation dans l'index du connecteur, et le compte se trouve alors
     * dans la réponse comme n'importe quel autre. <strong>C'est ce qui rend l'écran praticable
     * quand nos données sont vides</strong> : personne n'est ingéré, on écrit son Riot ID entier,
     * il apparaît.</p>
     *
     * <p>Et l'index profite à tout le monde : le compte vérifié par l'un est trouvable par les
     * suivants sans que Riot soit redemandé.</p>
     *
     * <p>404 si Riot ne connaît pas ce Riot ID, 503 si le connecteur est muet. Ce sont les deux
     * seuls cas où cette route échoue : une saisie partielle qui ne ressemble à rien rend une
     * liste vide, ce qui n'est pas une erreur.</p>
     *
     * <h2>Pourquoi la recherche est dans le connecteur et le « pourquoi » ici</h2>
     *
     * <p>Le §4 de la migration tranche : <em>un connecteur ne sait pas pourquoi on l'appelle</em>.
     * Trouver un pseudo dans son index est une question sur <strong>ses</strong> données — il
     * détient les parties, les observations et leurs index. Savoir qu'un de ces comptes est
     * <em>déjà revendiqué par un autre compte Schub</em> est une question sur l'identité, qui vit
     * ici et dont le connecteur n'a jamais entendu parler. Chacun finit son propre travail : il
     * cherche, le cœur qualifie.</p>
     *
     * <p>Marquer plutôt qu'écarter les comptes déjà liés : les retirer ferait croire à une faute
     * de saisie, alors que le vrai message est « ce compte est pris ».</p>
     */
    public List<RiotAccountSuggestionDto> suggestions(User actor, String query, int limit) {
        RiotId aVerifier = RiotId.completOuNull(query);
        if (aVerifier != null) {
            verifieAupresDeRiot(aVerifier);
        }

        List<RiotConnectorService.KnownPlayer> trouves = riotConnectorService.search(query, limit);
        if (trouves.isEmpty()) {
            return List.of();
        }

        Set<String> puuids = trouves.stream()
                .map(RiotConnectorService.KnownPlayer::puuid)
                .filter(puuid -> puuid != null && !puuid.isBlank())
                .collect(Collectors.toSet());
        Map<String, User> revendiques = puuids.isEmpty() ? Map.of()
                : userRepository.findByRiotPuuidIn(puuids).stream()
                        .collect(Collectors.toMap(User::getRiotPuuid, Function.identity(),
                                (premier, second) -> premier));

        return trouves.stream().map(joueur -> {
            User proprietaire = revendiques.get(joueur.puuid());
            boolean mien = proprietaire != null && proprietaire.getId().equals(actor.getId());
            return new RiotAccountSuggestionDto(
                    joueur.riotId(),
                    joueur.gameName(),
                    joueur.tagLine(),
                    joueur.matchCount(),
                    joueur.positions().stream()
                            .map(poste -> new RiotAccountSuggestionDto.PositionPlayedDto(
                                    poste.position(), poste.matches()))
                            .toList(),
                    joueur.lastPlayedAt(),
                    joueur.observedAt(),
                    joueur.source(),
                    proprietaire != null && !mien,
                    mien);
        }).toList();
    }

    /**
     * Le geste « va demander à Riot ». Il n'écrit rien sur le {@code User} et ne lie personne :
     * il fait exister le compte dans l'index, et c'est le clic qui reste le seul geste de liaison.
     */
    private void verifieAupresDeRiot(RiotId riotId) {
        RiotIdResolution resolution = riotIdResolver.resolve(riotId.gameName(), riotId.tagLine());
        if (resolution.isNotFound()) {
            throw new UnknownRiotAccountException(riotId.riotId());
        }
        if (resolution.puuid() == null) {
            throw new RiotConnectorUnavailableException();
        }
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

    /**
     * Demande la collecte au connecteur dès qu'un {@code puuid} nouveau est connu.
     *
     * <p>Sans elle, un compte lié resterait sans la moindre partie jusqu'à ce que quelqu'un
     * déclenche la collecte à la main — et personne ne le ferait, puisque rien ne le dit.</p>
     */
    private boolean demandeLaCollecte(User acteur, String ancienPuuid, String puuid) {
        if (puuid == null || puuid.equals(ancienPuuid)) {
            return false;
        }
        boolean demandee = riotConnectorService.requestIngest(puuid);
        if (demandee) {
            log.info("Collecte des parties demandée pour le compte {}", acteur.getDiscordId());
        }
        return demandee;
    }

    private RiotAccountChangeDto changement(RiotId ancien, RiotId nouveau, boolean collecteDemandee) {
        return new RiotAccountChangeDto(
                ancien == null ? null : ancien.riotId(),
                nouveau.riotId(),
                true,
                collecteDemandee,
                PARTIES_ESTIMEES,
                DUREE_ESTIMEE,
                true);
    }

    private RiotIngestDto ingestOf(User user) {
        String puuid = trimOrNull(user.getRiotPuuid());
        if (puuid == null) {
            return null;
        }
        return riotConnectorService.ingestOf(puuid)
                .map(ingest -> new RiotIngestDto(ingest.pending(), ingest.running(), ingest.estimatedReadyAt()))
                .orElse(null);
    }

    private RiotAccountDto toDto(User user) {
        RiotId riotId = RiotId.deOuNull(user.getRiotGameName(), user.getRiotTagLine());
        if (riotId == null) {
            return new RiotAccountDto(RiotAccountState.ABSENT, null, null, null, null, null, null);
        }
        RiotAccountState state = trimOrNull(user.getRiotPuuid()) == null
                ? RiotAccountState.EN_ATTENTE_DE_RESOLUTION
                : RiotAccountState.RESOLU;
        return new RiotAccountDto(state, riotId.riotId(), riotId.gameName(), riotId.tagLine(),
                user.getRiotLinkedAt(), null, null);
    }

    private static String trimOrNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
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

        /**
         * Le même découpage, mais sans refus : {@code null} veut dire « ce n'est pas un Riot ID
         * complet », donc une saisie à chercher et non un compte à vérifier.
         */
        static RiotId completOuNull(String saisie) {
            try {
                return parse(saisie);
            } catch (IllegalArgumentException pasUnRiotId) {
                return null;
            }
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
