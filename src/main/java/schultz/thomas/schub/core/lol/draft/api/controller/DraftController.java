package schultz.thomas.schub.core.lol.draft.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import schultz.thomas.schub.core.api.controller.CoreHeaders;
import schultz.thomas.schub.core.business.service.UserService;
import schultz.thomas.schub.core.lol.draft.api.dto.CompositionRecordDto;
import schultz.thomas.schub.core.lol.draft.api.dto.DraftAdviceDto;
import schultz.thomas.schub.core.lol.draft.api.mapper.DraftDtoMapper;
import schultz.thomas.schub.core.lol.draft.business.service.CompositionRecordService;
import schultz.thomas.schub.core.lol.draft.business.service.DraftAdviceService;
import schultz.thomas.schub.core.lol.stats.business.service.StatsWindows;

import java.util.List;

@RestController
@RequestMapping("/teams/{teamId}/stats")
@RequiredArgsConstructor
public class DraftController {

    private final DraftAdviceService draftAdvice;
    private final CompositionRecordService compositionRecords;
    private final UserService userService;
    private final StatsWindows windows;
    private final DraftDtoMapper dtos;

    @GetMapping("/draft")
    public DraftAdviceDto draft(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId,
            @PathVariable String teamId,
            @RequestParam(required = false) Integer days,
            @RequestParam(required = false) Integer patches) {
        return dtos.toDto(draftAdvice.of(userService.requireActor(actorId), teamId, windows.days(days, patches)));
    }

    @GetMapping("/compositions")
    public List<CompositionRecordDto> compositions(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId,
            @PathVariable String teamId,
            @RequestParam(required = false) Integer days,
            @RequestParam(required = false) Integer patches) {
        return dtos.toCompositionRecordDtos(
                compositionRecords.of(userService.requireActor(actorId), teamId, windows.days(days, patches)));
    }
}
