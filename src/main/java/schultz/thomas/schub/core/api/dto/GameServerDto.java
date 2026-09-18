package schultz.thomas.schub.core.api.dto;

import java.time.Instant;
import java.util.List;

/**
 * Un GameServer tel que le cœur l'expose à qui détient {@code SERVER_INFRA_VIEW} — la projection
 * <strong>infra</strong>, la plus complète des trois.
 *
 * <p>Vocabulaire du §2 : {@code slug} remplace {@code identifier}, {@code deploymentId} remplace
 * {@code portainerStackId}. Le cœur ne nomme plus la marque de l'outil qui réalise le serveur.</p>
 *
 * <p>Les deux autres projections sont {@link GameServerMemberDto} (tout compte connecté) et
 * {@link PublicServerStatusDto} (sans compte). Ce découpage est un contrat d'API : il se décide
 * en écrivant le modèle de droits, parce que le changer ensuite casse le front (plan §A.1).</p>
 *
 * <p>Ce même type sert d'entrée aux {@code POST} et {@code PUT} : {@code admins} y désigne les
 * comptes par leur seul {@code userId}, le reste de {@link ServerAdminDto} n'étant renseigné
 * qu'en sortie.</p>
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
        /** ids internes de comptes, résolus en pseudo et avatar pour l'affichage (plan §A.4) */
        List<ServerAdminDto> admins,
        /** ports pilotés sur le routeur ; absent d'un PUT = ports inchangés */
        List<GameServerPortDto> ports,
        /** null = jamais observé ; UNKNOWN/ONLINE/OFFLINE/UNREACHABLE sinon */
        String status,
        Instant lastStatusCheckAt,
        Instant lastStatusChangeAt,
        List<GameServerStatusHistoryEntryDto> statusHistory
) {
}
