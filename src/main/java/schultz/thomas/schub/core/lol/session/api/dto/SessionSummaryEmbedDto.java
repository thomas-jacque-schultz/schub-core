package schultz.thomas.schub.core.lol.session.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

// Un encadré Discord du résumé de session, tel que l'aperçu l'affiche.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SessionSummaryEmbedDto(String title, String url, String description, Integer color,
                                     List<FieldDto> fields, FooterDto footer) {

    public record FieldDto(String name, String value, boolean inline) {
    }

    public record FooterDto(String text) {
    }
}
