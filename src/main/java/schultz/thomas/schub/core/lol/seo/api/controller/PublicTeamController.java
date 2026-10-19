package schultz.thomas.schub.core.lol.seo.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import schultz.thomas.schub.core.lol.seo.api.dto.PublicTeamDto;
import schultz.thomas.schub.core.lol.seo.api.mapper.SeoDtoMapper;
import schultz.thomas.schub.core.lol.seo.business.service.PublicTeamService;

import java.util.List;

@RestController
@RequestMapping("/public-teams")
@RequiredArgsConstructor
public class PublicTeamController {

    private final PublicTeamService publicTeams;
    private final SeoDtoMapper dtos;

    // Pour le plan du site.
    @GetMapping
    public List<PublicTeamDto.Listed> listed() {
        return dtos.toListedDtos(publicTeams.listed());
    }

    @GetMapping("/{teamId}")
    public PublicTeamDto byId(@PathVariable String teamId) {
        return dtos.toDto(publicTeams.of(teamId));
    }
}
