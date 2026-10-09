package com.proyecto.servicios.config;

import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.security.RutasPublicas;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class OpenApi {

    private static final String ESQUEMA_BEARER = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info().title("API de Onboarding de Clientes").version("1.0"))
                .components(new Components().addSecuritySchemes(ESQUEMA_BEARER,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Pega aquí el jwtToken que devuelve POST /auth/login")));
    }

    /**
     * A TODOS los endpoints les documenta 400 y 500 (y 401 + candado a los protegidos) con el
     * formato { codigo, mensaje }. Así Swagger ya no muestra "Undocumented" en esas respuestas.
     */
    @Bean
    @SuppressWarnings({"rawtypes", "unchecked"})
    public OpenApiCustomizer respuestasYSeguridadComunes() {
        return openApi -> {
            if (openApi.getComponents() == null) {
                openApi.setComponents(new Components());
            }
            Map<String, Schema> esquemas = ModelConverters.getInstance().read(GenericResponse.class);
            esquemas.forEach((nombre, esquema) -> {
                if (openApi.getComponents().getSchemas() == null
                        || !openApi.getComponents().getSchemas().containsKey(nombre)) {
                    openApi.getComponents().addSchemas(nombre, esquema);
                }
            });

            Schema<Object> referencia = new Schema<>();
            referencia.set$ref("#/components/schemas/GenericResponse");

            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().forEach((ruta, item) ->
                    item.readOperationsMap().forEach((metodo, operacion) -> {
                        if (operacion.getResponses() == null) {
                            operacion.setResponses(new ApiResponses());
                        }
                        agregarSiFalta(operacion.getResponses(), "400", "Bad Request", referencia);
                        agregarSiFalta(operacion.getResponses(), "500", "Internal Server Error", referencia);
                        if (!RutasPublicas.esPublica(metodo.name(), ruta)) {
                            agregarSiFalta(operacion.getResponses(), "401", "Unauthorized", referencia);
                            operacion.addSecurityItem(new SecurityRequirement().addList(ESQUEMA_BEARER));
                        }
                    }));
        };
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void agregarSiFalta(ApiResponses respuestas, String codigo, String descripcion, Schema referencia) {
        if (respuestas.containsKey(codigo)) {
            return;
        }
        respuestas.addApiResponse(codigo, new ApiResponse()
                .description(descripcion)
                .content(new Content().addMediaType("application/json",
                        new MediaType().schema(referencia))));
    }
}
