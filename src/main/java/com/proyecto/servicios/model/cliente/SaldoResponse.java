package com.proyecto.servicios.model.cliente;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class SaldoResponse {
    private BigDecimal saldoDisponible;
    private LocalDateTime fechaActualizacion;
}