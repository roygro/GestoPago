package com.proyecto.servicios.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

/** Envoltorio común de todas las respuestas exitosas de la API. */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RespuestaApi<T> {

    private Integer codigo;
    private String mensaje;
    private T datos;
    private LocalDateTime timestamp = LocalDateTime.now();

    public RespuestaApi(Integer codigo, String mensaje, T datos) {
        this.codigo = codigo;
        this.mensaje = mensaje;
        this.datos = datos;
    }

    public static <T> RespuestaApi<T> de(HttpStatus status, String mensaje, T datos) {
        return new RespuestaApi<>(status.value(), mensaje, datos);
    }
}