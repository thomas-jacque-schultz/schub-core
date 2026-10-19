package schultz.thomas.schub.core.lol.seo.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import schultz.thomas.schub.core.lol.seo.api.dto.PlayerCollectDto;
import schultz.thomas.schub.core.lol.seo.api.dto.PublicTeamDto;
import schultz.thomas.schub.core.lol.seo.api.dto.SearchedPlayerDto;
import schultz.thomas.schub.core.lol.seo.business.model.PlayerCollect;
import schultz.thomas.schub.core.lol.seo.business.model.PublicTeam;
import schultz.thomas.schub.core.lol.seo.business.model.SearchedPlayer;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SeoDtoMapper {

    PlayerCollectDto toDto(PlayerCollect collect);

    SearchedPlayerDto toDto(SearchedPlayer player);

    PublicTeamDto toDto(PublicTeam team);

    List<PublicTeamDto.Listed> toListedDtos(List<PublicTeam.Listed> teams);
}
