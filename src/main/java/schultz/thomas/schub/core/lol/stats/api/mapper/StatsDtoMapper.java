package schultz.thomas.schub.core.lol.stats.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import schultz.thomas.schub.core.lol.stats.api.dto.CompanionsDto;
import schultz.thomas.schub.core.lol.stats.api.dto.MyGamesDto;
import schultz.thomas.schub.core.lol.stats.api.dto.MyStatsDto;
import schultz.thomas.schub.core.lol.stats.api.dto.StatsRefreshDto;
import schultz.thomas.schub.core.lol.stats.api.dto.TeamGameDetailDto;
import schultz.thomas.schub.core.lol.stats.api.dto.TeamGamesStatsDto;
import schultz.thomas.schub.core.lol.stats.api.dto.TeamOppositionDto;
import schultz.thomas.schub.core.lol.stats.api.dto.TeamPlayersStatsDto;
import schultz.thomas.schub.core.lol.stats.api.dto.TeamSynergyDto;
import schultz.thomas.schub.core.lol.stats.business.model.Companions;
import schultz.thomas.schub.core.lol.stats.business.model.MyGames;
import schultz.thomas.schub.core.lol.stats.business.model.MyStats;
import schultz.thomas.schub.core.lol.stats.business.model.StatsRefresh;
import schultz.thomas.schub.core.lol.stats.business.model.TeamGameDetail;
import schultz.thomas.schub.core.lol.stats.business.model.TeamGamesStats;
import schultz.thomas.schub.core.lol.stats.business.model.TeamOpposition;
import schultz.thomas.schub.core.lol.stats.business.model.TeamPlayersStats;
import schultz.thomas.schub.core.lol.stats.business.model.TeamSynergy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface StatsDtoMapper {

    MyStatsDto toDto(MyStats stats);

    MyGamesDto toDto(MyGames games);

    TeamGameDetailDto toDto(TeamGameDetail game);

    CompanionsDto toDto(Companions companions);

    TeamPlayersStatsDto toDto(TeamPlayersStats stats);

    TeamGamesStatsDto toDto(TeamGamesStats stats);

    TeamOppositionDto toDto(TeamOpposition opposition);

    StatsRefreshDto toDto(StatsRefresh refresh);

    TeamSynergyDto toDto(TeamSynergy synergy);
}
