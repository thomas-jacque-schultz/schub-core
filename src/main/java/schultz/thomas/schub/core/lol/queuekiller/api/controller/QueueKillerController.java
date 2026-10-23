package schultz.thomas.schub.core.lol.queuekiller.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import schultz.thomas.schub.core.api.controller.CoreHeaders;
import schultz.thomas.schub.core.business.service.UserService;
import schultz.thomas.schub.core.lol.queuekiller.api.dto.PremadeRoomDto;
import schultz.thomas.schub.core.lol.queuekiller.api.dto.PresenceRequest;
import schultz.thomas.schub.core.lol.queuekiller.business.service.QueueKillerPresenceService;

import java.util.List;

@RestController
@RequestMapping("/queue-killer")
@RequiredArgsConstructor
public class QueueKillerController {

    private final QueueKillerPresenceService presences;
    private final UserService userService;

    @PutMapping("/presence")
    public ResponseEntity<Void> signal(@RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId,
                                       @RequestBody PresenceRequest request) {
        presences.signal(userService.requireActor(actorId), request.game(), request.room());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/presence/{game}/{room}")
    public ResponseEntity<Void> leave(@RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId,
                                      @PathVariable String game, @PathVariable String room) {
        presences.leave(userService.requireActor(actorId), game, room);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/premade-rooms")
    public List<PremadeRoomDto> premadeRooms(
            @RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId) {
        return presences.premadeRooms(userService.requireActor(actorId)).stream().map(PremadeRoomDto::of).toList();
    }
}
