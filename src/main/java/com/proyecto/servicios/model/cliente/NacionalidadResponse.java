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
public class NacionalidadResponse {

    @Schema(description = "Valor que se envía en el campo nacionalidad al registrar un cliente (tal cual, con acentos)", example = "Mexicano")
    private String nacionalidad;

    @Schema(description = "País", example = "México")
    private String pais;
}
