package com.proyecto.servicios.model.cliente;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ClienteIdRequest {

    @NotNull(message = "El id del cliente es obligatorio")
    @Positive(message = "El id del cliente debe ser mayor a cero")
    @Schema(example = "1")
    private Long id;
}