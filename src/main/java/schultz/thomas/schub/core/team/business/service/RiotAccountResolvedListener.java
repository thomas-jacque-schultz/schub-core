package schultz.thomas.schub.core.team.business.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import schultz.thomas.schub.core.business.service.RiotAccountResolved;
import schultz.thomas.schub.core.data.model.User;

@Slf4j
@Component
@RequiredArgsConstructor
public class RiotAccountResolvedListener {

    private final TeamService teamService;

    @EventListener
    public void onResolved(RiotAccountResolved event) {
        User porteur = new User();
        porteur.setId(event.userId());
        porteur.setDiscordId(event.discordId());
        porteur.setRiotPuuid(event.puuid());
        porteur.setRiotGameName(event.gameName());
        porteur.setRiotTagLine(event.tagLine());

        int rattachees = teamService.claim(porteur).size();
        if (rattachees > 0) {
            log.info("{} place(s) d'effectif rattachée(s) après résolution du compte Riot", rattachees);
        }
    }
}
