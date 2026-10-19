package schultz.thomas.schub.core.api.client;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import schultz.thomas.schub.core.api.client.riot.AccountsByRankDto;
import schultz.thomas.schub.core.api.client.riot.CrawlerStatusDto;
import schultz.thomas.schub.core.api.client.riot.HistoryWindowDto;
import schultz.thomas.schub.core.api.client.riot.IngestStatusDto;
import schultz.thomas.schub.core.api.client.riot.IngestSummaryDto;
import schultz.thomas.schub.core.api.client.riot.PlayerIngestStatusDto;
import schultz.thomas.schub.core.api.client.riot.PlayerSuggestionDto;
import schultz.thomas.schub.core.business.service.RiotConnectorService;

import java.time.Duration;

// Le contrat du connecteur Riot (modèles générés) vers le port de la collecte (Schub#124).
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface RiotIngestContractMapper {

    RiotConnectorService.PlayerIngest toIngest(PlayerIngestStatusDto ingest);

    RiotConnectorService.IngestLoad toLoad(IngestStatusDto load);

    RiotConnectorService.IngestSummary toSummary(IngestSummaryDto summary);

    RiotConnectorService.Crawler toCrawler(CrawlerStatusDto crawler);

    RiotConnectorService.AccountsByRank toAccounts(AccountsByRankDto accounts);

    RiotConnectorService.HistoryWindow toWindow(HistoryWindowDto window);

    HistoryWindowDto toDto(RiotConnectorService.HistoryWindow window);

    RiotConnectorService.KnownPlayer toKnownPlayer(PlayerSuggestionDto suggestion);

    default Duration duree(String iso) {
        return iso == null ? null : Duration.parse(iso);
    }
}
