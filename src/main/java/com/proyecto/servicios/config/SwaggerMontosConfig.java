package com.proyecto.servicios.config;

import io.swagger.v3.oas.models.media.StringSchema;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

/**
 * OPCIONAL (solo documentación). Como las cantidades salen como texto "2000.00" (ver DosDecimalesSerializer),
 * aquí se le dice a Swagger que documente todo BigDecimal como texto con ejemplo "2000.00".
 * Si da error de compilación en el import de SpringDocUtils, borra este archivo: la API funciona igual.
 */
@Configuration
public class SwaggerMontosConfig {

    static {
        SpringDocUtils.getConfig().replaceWithSchema(BigDecimal.class,
                new StringSchema().example("2000.00").description("Cantidad con 2 decimales"));
    }
}
