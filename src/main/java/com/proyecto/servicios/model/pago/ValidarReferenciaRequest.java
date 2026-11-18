package com.proyecto.servicios.model.pago;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidarReferenciaRequest {

    @NotNull(message = "El id de servicio es obligatorio")
    private Integer idServicio;

    @NotNull(message = "El id de producto es obligatorio")
    private Integer idProducto;

    private String referencia;

    private String telefono;
}
