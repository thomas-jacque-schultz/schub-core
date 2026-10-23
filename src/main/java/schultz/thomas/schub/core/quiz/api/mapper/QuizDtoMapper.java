package schultz.thomas.schub.core.quiz.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import schultz.thomas.schub.core.quiz.api.dto.QuizQuestionDto;
import schultz.thomas.schub.core.quiz.api.dto.QuizQuestionRequest;
import schultz.thomas.schub.core.quiz.business.model.QuizQuestionDraft;
import schultz.thomas.schub.core.quiz.data.model.QuizQuestion;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface QuizDtoMapper {

    QuizQuestionDto toDto(QuizQuestion question);

    QuizQuestionDraft toDraft(QuizQuestionRequest request);

    default List<QuizQuestionDto> toDtos(List<QuizQuestion> questions) {
        return questions.stream().map(this::toDto).toList();
    }
}
