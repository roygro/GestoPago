package com.proyecto.servicios.model.pago;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidarReferenciaResponse {
    private boolean valida;
    private String mensaje;
    private String servicio;
    private String producto;
    private String tipoReferencia;
    private BigDecimal montoSugerido;
    private String referenciaIngresada;
}
