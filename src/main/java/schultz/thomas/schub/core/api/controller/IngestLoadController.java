package schultz.thomas.schub.core.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import schultz.thomas.schub.core.api.dto.CrawlerDto;
import schultz.thomas.schub.core.api.dto.CrawlerToggleRequest;
import schultz.thomas.schub.core.api.dto.IngestLoadDto;
import schultz.thomas.schub.core.business.model.Permission;
import schultz.thomas.schub.core.business.service.PermissionEvaluator;
import schultz.thomas.schub.core.business.service.RiotConnectorService;
import schultz.thomas.schub.core.business.service.UserService;

@RestController
@RequestMapping("/ingest")
@RequiredArgsConstructor
public class IngestLoadController {

    private final RiotConnectorService riotConnector;
    private final PermissionEvaluator permissionEvaluator;
    private final UserService userService;

    @GetMapping("/load")
    public ResponseEntity<IngestLoadDto> load(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId) {
        permissionEvaluator.require(userService.requireActor(actorDiscordId), Permission.INGEST_VIEW, null);

        return ResponseEntity.ok(riotConnector.load()
                .map(IngestLoadDto::from)
                .orElseGet(IngestLoadDto::unavailable));
    }

    @GetMapping("/crawler")
    public ResponseEntity<CrawlerDto> crawler(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId) {
        permissionEvaluator.require(userService.requireActor(actorDiscordId), Permission.INGEST_VIEW, null);

        return ResponseEntity.ok(riotConnector.crawler()
                .map(CrawlerDto::from)
                .orElseGet(CrawlerDto::unavailable));
    }

    @PutMapping("/crawler")
    public ResponseEntity<CrawlerDto> toggleCrawler(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorDiscordId,
            @RequestBody CrawlerToggleRequest request) {
        permissionEvaluator.require(userService.requireActor(actorDiscordId), Permission.INGEST_MANAGE, null);

        return ResponseEntity.ok(CrawlerDto.from(riotConnector.toggleCrawler(request.enabled())));
    }
}
