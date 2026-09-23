package schultz.thomas.schub.core.business.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
 * La propriété du compte Riot n'est pas vérifiée : le connecteur confirme seulement que le Riot ID existe.
 * La vérification réelle (RSO) demande une approbation Riot séparée, hors périmètre.
 * Ne touche jamais aux équipes : team lit l'identité, jamais l'inverse.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiotAccountService {

    private static final int PARTIES_ESTIMEES = 1_000;

    private static final Duration DUREE_ESTIMEE = Duration.ofMinutes(20);

    private final UserRepository userRepository;
    private final RiotIdResolver riotIdResolver;
    private final RiotConnectorService riotConnectorService;
    private final ApplicationEventPublisher events;

    public RiotAccountDto of(User actor) {
        return toDto(actor).withIngest(ingestOf(actor));
    }

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
        if (puuid != null && !puuid.equals(ancienPuuid)) {
            events.publishEvent(new RiotAccountResolved(enregistre.getId(), enregistre.getDiscordId(),
                enregistre.getRiotPuuid(), enregistre.getRiotGameName(), enregistre.getRiotTagLine()));
        }

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

    public boolean resolvePendingLink(User actor) {
        if (actor == null || trimOrNull(actor.getRiotPuuid()) != null) {
            return false;
        }
        String gameName = trimOrNull(actor.getRiotGameName());
        String tagLine = trimOrNull(actor.getRiotTagLine());
        if (gameName == null || tagLine == null) {
            return false;
        }

        RiotIdResolution resolution = riotIdResolver.resolve(gameName, tagLine);
        if (resolution.puuid() == null) {
            return false;
        }
        if (userRepository.findByRiotPuuid(resolution.puuid())
                .filter(autre -> !autre.getId().equals(actor.getId())).isPresent()) {
            log.warn("Lien en attente de {} non résolu : le puuid est déjà revendiqué ailleurs",
                    actor.getDiscordId());
            return false;
        }

        actor.setRiotPuuid(resolution.puuid());
        User enregistre = userRepository.save(actor);
        demandeLaCollecte(enregistre, null, resolution.puuid());
        events.publishEvent(new RiotAccountResolved(enregistre.getId(), enregistre.getDiscordId(),
                enregistre.getRiotPuuid(), enregistre.getRiotGameName(), enregistre.getRiotTagLine()));
        log.info("Lien en attente de {} résolu à la connexion : {}#{}",
                actor.getDiscordId(), gameName, tagLine);
        return true;
    }

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

    private void verifieAupresDeRiot(RiotId riotId) {
        RiotIdResolution resolution = riotIdResolver.resolve(riotId.gameName(), riotId.tagLine());
        if (resolution.isNotFound()) {
            throw new UnknownRiotAccountException(riotId.riotId());
        }
        if (resolution.isBusy()) {
            throw new RiotConnectorBusyException();
        }
        if (resolution.puuid() == null) {
            throw new RiotConnectorUnavailableException();
        }
    }

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

    // Découpe sur le premier # ; un second # est une faute de saisie (un gameName Riot n'en contient pas).
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
