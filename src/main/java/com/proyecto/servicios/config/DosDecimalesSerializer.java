package com.proyecto.servicios.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.boot.jackson.JsonComponent;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Toda cantidad (BigDecimal) que sale en un JSON lleva exactamente 2 decimales: 2000 -> "2000.00",
 * 2000.5 -> "2000.50". Aplica a ingresoMensual, saldoDisponible, precio... sin importar cómo se construyó
 * el objeto (aunque venga del request y no de la BD).
 *
 * Sale como TEXTO ("2000.00") y no como número a propósito: Swagger UI (y cualquier cliente JavaScript)
 * vuelve a interpretar los números JSON y los imprime sin ceros finales (2000.00 -> 2000). Como texto se ve
 * exactamente igual que lo manda el servidor. Al RECIBIR, el API sigue aceptando 2000, 2000.0 o "2000.00".
 * Spring Boot lo registra solo por la anotación @JsonComponent.
 */
@JsonComponent
public class DosDecimalesSerializer extends JsonSerializer<BigDecimal> {

    @Override
    public void serialize(BigDecimal valor, JsonGenerator generador, SerializerProvider proveedor) throws IOException {
        // toPlainString evita la notación científica (1E+3)
        generador.writeString(valor.setScale(2, RoundingMode.HALF_UP).toPlainString());
    }
}
