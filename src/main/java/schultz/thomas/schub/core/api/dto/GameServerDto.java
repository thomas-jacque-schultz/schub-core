package schultz.thomas.schub.core.api.dto;

import schultz.thomas.schub.core.data.model.GameServer;

import java.time.Instant;
import java.util.List;

/**
 * Un GameServer tel que le cœur l'expose.
 *
 * <p>Vocabulaire du §2 : {@code slug} remplace {@code identifier}, {@code deploymentId} remplace
 * {@code portainerStackId}. Le cœur ne nomme plus la marque de l'outil qui réalise le serveur.</p>
 */
public record GameServerDto(
        String id,
        /** identifiant humain, stable, utilisé dans les URLs et les commandes */
        String slug,
        /** le déploiement qui réalise ce serveur */
        Integer deploymentId,
        String name,
        String urlConnection,
        String game,
        /** libellé et icône, dérivés du jeu : évite aux consommateurs de dupliquer le catalogue */
        String gameLabel,
        String gameIconUrl,
        Integer playersMax,
        String installation,
        String version,
        String description,
        List<String> admins,
        /** ports pilotés sur le routeur ; absent d'un PUT = ports inchangés */
        List<GameServerPortDto> ports,
        /** null = jamais observé ; UNKNOWN/ONLINE/OFFLINE/UNREACHABLE sinon */
        String status,
        Instant lastStatusCheckAt,
        Instant lastStatusChangeAt,
        List<GameServerStatusHistoryEntryDto> statusHistory
) {
}
