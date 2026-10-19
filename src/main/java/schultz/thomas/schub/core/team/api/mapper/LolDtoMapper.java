package schultz.thomas.schub.core.team.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import schultz.thomas.schub.core.team.api.dto.ChampionGridDto;
import schultz.thomas.schub.core.team.api.dto.ChampionPoolDto;
import schultz.thomas.schub.core.team.api.dto.CompanionsDto;
import schultz.thomas.schub.core.team.api.dto.CompositionRecordDto;
import schultz.thomas.schub.core.team.api.dto.DraftAdviceDto;
import schultz.thomas.schub.core.team.api.dto.IngestPauseDto;
import schultz.thomas.schub.core.team.api.dto.MyGamesDto;
import schultz.thomas.schub.core.team.api.dto.MyStatsDto;
import schultz.thomas.schub.core.team.api.dto.PlayerCollectDto;
import schultz.thomas.schub.core.team.api.dto.PublicTeamDto;
import schultz.thomas.schub.core.team.api.dto.ReferenceGridDto;
import schultz.thomas.schub.core.team.api.dto.RiotDataInvalidationDto;
import schultz.thomas.schub.core.team.api.dto.RiotDataInventoryDto;
import schultz.thomas.schub.core.team.api.dto.SearchedPlayerDto;
import schultz.thomas.schub.core.team.api.dto.StatsRefreshDto;
import schultz.thomas.schub.core.team.api.dto.TeamGameDetailDto;
import schultz.thomas.schub.core.team.api.dto.TeamGamesStatsDto;
import schultz.thomas.schub.core.team.api.dto.TeamOppositionDto;
import schultz.thomas.schub.core.team.api.dto.TeamPlayersStatsDto;
import schultz.thomas.schub.core.team.api.dto.TeamSynergyDto;
import schultz.thomas.schub.core.team.api.dto.TierCombosDto;
import schultz.thomas.schub.core.team.business.model.ChampionGrid;
import schultz.thomas.schub.core.team.business.model.ChampionPool;
import schultz.thomas.schub.core.team.business.model.Companions;
import schultz.thomas.schub.core.team.business.model.CompositionRecord;
import schultz.thomas.schub.core.team.business.model.DraftAdvice;
import schultz.thomas.schub.core.team.business.model.IngestPauseState;
import schultz.thomas.schub.core.team.business.model.MyGames;
import schultz.thomas.schub.core.team.business.model.MyStats;
import schultz.thomas.schub.core.team.business.model.PlayerCollect;
import schultz.thomas.schub.core.team.business.model.PublicTeam;
import schultz.thomas.schub.core.team.business.model.ReferenceGrid;
import schultz.thomas.schub.core.team.business.model.RiotDataInvalidation;
import schultz.thomas.schub.core.team.business.model.RiotDataInventory;
import schultz.thomas.schub.core.team.business.model.SearchedPlayer;
import schultz.thomas.schub.core.team.business.model.StatsRefresh;
import schultz.thomas.schub.core.team.business.model.TeamGameDetail;
import schultz.thomas.schub.core.team.business.model.TeamGamesStats;
import schultz.thomas.schub.core.team.business.model.TeamOpposition;
import schultz.thomas.schub.core.team.business.model.TeamPlayersStats;
import schultz.thomas.schub.core.team.business.model.TeamSynergy;
import schultz.thomas.schub.core.team.business.model.TierCombos;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface LolDtoMapper {

    ChampionPoolDto toDto(ChampionPool pool);

    MyStatsDto toDto(MyStats stats);

    MyGamesDto toDto(MyGames games);

    TeamGameDetailDto toDto(TeamGameDetail game);

    PlayerCollectDto toDto(PlayerCollect collect);

    SearchedPlayerDto toDto(SearchedPlayer player);

    CompanionsDto toDto(Companions companions);

    PublicTeamDto toDto(PublicTeam team);

    List<PublicTeamDto.Listed> toListedDtos(List<PublicTeam.Listed> teams);

    ChampionGridDto toDto(ChampionGrid grid);

    ReferenceGridDto toDto(ReferenceGrid grid);

    IngestPauseDto toDto(IngestPauseState pause);

    RiotDataInventoryDto toDto(RiotDataInventory inventory);

    RiotDataInvalidationDto toDto(RiotDataInvalidation invalidation);

    TeamPlayersStatsDto toDto(TeamPlayersStats stats);

    TeamGamesStatsDto toDto(TeamGamesStats stats);

    TeamOppositionDto toDto(TeamOpposition opposition);

    DraftAdviceDto toDto(DraftAdvice advice);

    List<CompositionRecordDto> toCompositionRecordDtos(List<CompositionRecord> records);

    StatsRefreshDto toDto(StatsRefresh refresh);

    TeamSynergyDto toDto(TeamSynergy synergy);

    TierCombosDto toDto(TierCombos combos);
}
