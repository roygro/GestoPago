package com.proyecto.servicios.model.cliente;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EstadoCivilResponse {

    @Schema(description = "Clave que se envía en el campo estadoCivil al registrar un cliente", example = "SOLTERO")
    private String clave;

    @Schema(description = "Texto para mostrar al usuario", example = "Soltero(a)")
    private String descripcion;
}
