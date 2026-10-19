package schultz.thomas.schub.core.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import schultz.thomas.schub.core.api.dto.DisplayNameRequest;
import schultz.thomas.schub.core.api.dto.MeDto;
import schultz.thomas.schub.core.api.mapper.UserDtoMapper;
import schultz.thomas.schub.core.business.service.UserService;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.team.business.service.SessionSummaryService;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/me")
@RequiredArgsConstructor
public class MeController {

    private final UserService userService;
    private final UserDtoMapper users;
    private final SessionSummaryService sessionSummaries;

    // L'aperçu du résumé de session qu'aurait reçu ce compte pour ce jour (Schub#99) : rien n'est envoyé.
    @GetMapping("/session-summary")
    public ResponseEntity<Map<String, Object>> sessionSummary(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId,
            @RequestParam LocalDate day) {
        return sessionSummaries.preview(userService.requireActor(actorId), day)
                .map(encadres -> ResponseEntity.ok(Map.<String, Object>of("embeds", encadres)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping
    public MeDto me(@RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId) {
        return users.toMeDto(userService.requireActor(actorId));
    }

    @PutMapping("/session-summaries")
    public MeDto changeSessionSummaries(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId,
            @RequestBody MeDto.SessionSummariesDto request) {
        User actor = userService.requireActor(actorId);
        return users.toMeDto(userService.changeSessionSummaries(actor, request.planned(), request.free()));
    }

    @PutMapping("/display-name")
    public MeDto changeDisplayName(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId,
            @RequestBody DisplayNameRequest request) {
        User actor = userService.requireActor(actorId);
        return users.toMeDto(userService.changeDisplayName(actor, request.displayName()));
    }
}
