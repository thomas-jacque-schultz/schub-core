package schultz.thomas.schub.core.team.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import schultz.thomas.schub.core.team.api.dto.PublicTeamDto;
import schultz.thomas.schub.core.team.business.service.PublicTeamService;

import java.util.List;

@RestController
@RequestMapping("/public-teams")
@RequiredArgsConstructor
public class PublicTeamController {

    private final PublicTeamService publicTeams;

    // Pour le plan du site.
    @GetMapping
    public List<PublicTeamDto.Listed> listed() {
        return publicTeams.listed();
    }

    @GetMapping("/{teamId}")
    public PublicTeamDto byId(@PathVariable String teamId) {
        return publicTeams.of(teamId);
    }
}
