package schultz.thomas.schub.core.quiz.api.controller;

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
import schultz.thomas.schub.core.quiz.api.dto.QuizQuestionDto;
import schultz.thomas.schub.core.quiz.api.dto.QuizQuestionRequest;
import schultz.thomas.schub.core.quiz.api.mapper.QuizDtoMapper;
import schultz.thomas.schub.core.quiz.business.service.QuizQuestionService;

import java.util.List;

@RestController
@RequestMapping("/quiz/questions")
@RequiredArgsConstructor
public class QuizQuestionController {

    private final QuizQuestionService questions;
    private final UserService userService;
    private final QuizDtoMapper dtos;

    // Lue sans compte par le quiz : seules les questions actives.
    @GetMapping
    public List<QuizQuestionDto> active() {
        return dtos.toDtos(questions.active());
    }

    @GetMapping("/all")
    public List<QuizQuestionDto> all(@RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId) {
        return dtos.toDtos(questions.all(userService.requireActor(actorId)));
    }

    @PutMapping("/{id}")
    public QuizQuestionDto save(@RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId,
                                @PathVariable String id, @RequestBody QuizQuestionRequest request) {
        return dtos.toDto(questions.save(userService.requireActor(actorId), id, dtos.toDraft(request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@RequestHeader(value = CoreHeaders.ACTOR_ID, required = false) String actorId,
                                       @PathVariable String id) {
        questions.delete(userService.requireActor(actorId), id);
        return ResponseEntity.noContent().build();
    }
}
