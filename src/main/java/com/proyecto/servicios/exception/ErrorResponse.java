package com.proyecto.servicios.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private Integer codigo;
    private String mensaje;
    private List<CampoError> errores;   // solo aparece en errores de validación
    private LocalDateTime timestamp = LocalDateTime.now();

    public ErrorResponse(Integer codigo, String mensaje) {
        this.codigo = codigo;
        this.mensaje = mensaje;
    }

    public ErrorResponse(Integer codigo, String mensaje, List<CampoError> errores) {
        this(codigo, mensaje);
        this.errores = errores;
    }

    @Getter
    @AllArgsConstructor
    public static class CampoError {
        private String campo;
        private String mensaje;
    }
}