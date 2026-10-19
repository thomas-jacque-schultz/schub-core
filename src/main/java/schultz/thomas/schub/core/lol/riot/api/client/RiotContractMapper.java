package schultz.thomas.schub.core.lol.riot.api.client;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import schultz.thomas.schub.core.api.client.riot.ChampionCardDto;
import schultz.thomas.schub.core.api.client.riot.ChampionComboDto;
import schultz.thomas.schub.core.api.client.riot.ChampionMasteryDto;
import schultz.thomas.schub.core.api.client.riot.ChampionReferenceGridDto;
import schultz.thomas.schub.core.api.client.riot.IngestPauseStatusDto;
import schultz.thomas.schub.core.api.client.riot.MatchInsightsDto;
import schultz.thomas.schub.core.api.client.riot.MatchPlayerMetricsDto;
import schultz.thomas.schub.core.api.client.riot.ParticipationBucketDto;
import schultz.thomas.schub.core.api.client.riot.PatchStartDto;
import schultz.thomas.schub.core.api.client.riot.PlayerCoverageDto;
import schultz.thomas.schub.core.api.client.riot.PlayerReferencesDto;
import schultz.thomas.schub.core.api.client.riot.RankedStandingDto;
import schultz.thomas.schub.core.api.client.riot.ReferenceGridDto;
import schultz.thomas.schub.core.api.client.riot.RiotDataInventoryDto;
import schultz.thomas.schub.core.api.client.riot.SharedMatchDto;
import schultz.thomas.schub.core.api.client.riot.SharedMatchPlayerDto;
import schultz.thomas.schub.core.api.client.riot.Slot;
import schultz.thomas.schub.core.api.client.riot.TierCombosDto;
import schultz.thomas.schub.core.api.client.riot.TimelineHabitsDto;
import schultz.thomas.schub.core.lol.riot.business.model.ChampionGrid;
import schultz.thomas.schub.core.lol.riot.business.model.ReferenceGrid;
import schultz.thomas.schub.core.lol.riot.business.service.RiotChampionGateway;
import schultz.thomas.schub.core.lol.riot.business.service.RiotComboGateway;
import schultz.thomas.schub.core.lol.riot.business.service.RiotDataGateway;
import schultz.thomas.schub.core.lol.riot.business.service.RiotStatsGateway;

import java.util.List;

// Le contrat du connecteur Riot (modèles générés) vers les types de business. Un champ renommé ou retiré
// côté connecteur laisse une cible sans source : la compilation échoue ici (Schub#124).
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface RiotContractMapper {

    RiotStatsGateway.Bucket toBucket(ParticipationBucketDto bucket);

    RiotStatsGateway.Coverage toCoverage(PlayerCoverageDto coverage);

    RiotStatsGateway.SharedMatchPlayer toPlayer(SharedMatchPlayerDto player);

    RiotStatsGateway.References toReferences(PlayerReferencesDto references);

    RiotStatsGateway.PatchStart toPatchStart(PatchStartDto patch);

    RiotStatsGateway.PlayerMetrics toMetrics(MatchPlayerMetricsDto metrics);

    RiotStatsGateway.TimelineHabits toHabits(TimelineHabitsDto habits);

    RiotStatsGateway.Standing toStanding(RankedStandingDto standing);

    RiotStatsGateway.Insight toInsight(MatchInsightsDto insight);

    RiotChampionGateway.Champion toChampion(ChampionCardDto champion);

    RiotChampionGateway.Mastery toMastery(ChampionMasteryDto mastery);

    Slot toSlot(RiotComboGateway.Slot slot);

    RiotComboGateway.GeneralCombo toCombo(ChampionComboDto combo);

    RiotComboGateway.TierCombos toTierCombos(TierCombosDto combos);

    RiotDataGateway.IngestPause toPause(IngestPauseStatusDto pause);

    RiotDataGateway.Inventory toInventory(RiotDataInventoryDto inventory);

    ReferenceGrid toGrid(ReferenceGridDto grid);

    ChampionGrid toGrid(ChampionReferenceGridDto grid);

    // Les joueurs demandés d'un côté, les autres de l'autre.
    default RiotStatsGateway.SharedMatch toSharedMatch(SharedMatchDto match) {
        List<SharedMatchPlayerDto> tous = match.getPlayers() == null ? List.of() : match.getPlayers();
        return new RiotStatsGateway.SharedMatch(match.getMatchId(), match.getStartedAt(),
                valeur(match.getDurationSeconds()), valeur(match.getQueueId()),
                texte(match.getQueue()), match.getPatch(),
                valeur(match.getPresentPlayers()), Boolean.TRUE.equals(match.getSplitSides()), match.getWin(),
                tous.stream().filter(p -> Boolean.TRUE.equals(p.getRequested())).map(this::toPlayer).toList(),
                tous.stream().filter(p -> !Boolean.TRUE.equals(p.getRequested())).map(this::toPlayer).toList());
    }

    // La valeur JSON de l'enum ; une valeur que ce contrat ne connaît pas encore ne passe pas.
    default String texte(Enum<?> valeur) {
        return valeur == null || "unknown_default_open_api".equals(valeur.toString()) ? null : valeur.toString();
    }

    private static long valeur(Long nombre) {
        return nombre == null ? 0 : nombre;
    }

    private static int valeur(Integer nombre) {
        return nombre == null ? 0 : nombre;
    }
}
