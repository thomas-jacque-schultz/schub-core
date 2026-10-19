package schultz.thomas.schub.core.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import jakarta.annotation.Nullable;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Ce que le contrat dit de chaque champ des DTO (Schub#124), pour que le front en tire des types exacts :
// présent et non nul par défaut (Jackson écrit tous les composants d'un record) ; @Nullable : peut valoir null ;
// absent quand il est nul dans un DTO en NON_NULL, ou dans une requête.
@Component
public class ContractNullability implements OpenApiCustomizer {

    private static final String RACINE = "schultz.thomas.schub.core";

    private final Map<String, Class<?>> records = parNomDeSchema();

    @Override
    public void customise(OpenAPI openApi) {
        if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
            return;
        }
        openApi.getComponents().getSchemas().forEach((nom, schema) -> {
            Class<?> type = records.get(nom);
            if (type != null && schema.getProperties() != null) {
                applique(type, schema);
            }
        });
    }

    @SuppressWarnings("rawtypes")
    private static void applique(Class<?> type, Schema schema) {
        boolean omisSiNul = type.getSimpleName().endsWith("Request") || nonNull(type.getAnnotation(JsonInclude.class));
        List<String> requis = new ArrayList<>();
        for (RecordComponent composant : type.getRecordComponents()) {
            Schema propriete = (Schema) schema.getProperties().get(composant.getName());
            if (propriete == null) {
                continue;
            }
            boolean nullable = composant.isAnnotationPresent(Nullable.class);
            if (nullable && propriete.get$ref() != null) {
                // En OpenAPI 3.0, un $ref ignore ses voisins : nullable passe par un allOf.
                schema.getProperties().put(composant.getName(),
                        new Schema<>().allOf(List.of(new Schema<>().$ref(propriete.get$ref()))).nullable(true));
            } else if (nullable) {
                propriete.setNullable(true);
            }
            if (!(nullable && omisSiNul)) {
                requis.add(composant.getName());
            }
        }
        schema.setRequired(requis.isEmpty() ? null : requis);
    }

    private static boolean nonNull(JsonInclude inclusion) {
        return inclusion != null && inclusion.value() == JsonInclude.Include.NON_NULL;
    }

    private static Map<String, Class<?>> parNomDeSchema() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(
                    org.springframework.beans.factory.annotation.AnnotatedBeanDefinition definition) {
                return definition.getMetadata().isIndependent();
            }
        };
        scanner.addIncludeFilter(new AssignableTypeFilter(Record.class));
        Map<String, Class<?>> parNom = new HashMap<>();
        scanner.findCandidateComponents(RACINE).forEach(definition -> {
            if (!definition.getBeanClassName().contains(".api.")) {
                return;
            }
            Class<?> type = ClassUtils.resolveClassName(definition.getBeanClassName(), null);
            io.swagger.v3.oas.annotations.media.Schema nomme =
                    type.getAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);
            parNom.put(nomme != null && !nomme.name().isEmpty() ? nomme.name() : type.getSimpleName(), type);
        });
        return parNom;
    }
}
