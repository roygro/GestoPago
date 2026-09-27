package com.proyecto.servicios.model.cliente;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class CuentaResponse {
    private Long id;
    private String numeroCuenta;
    private LocalDateTime fechaApertura;
    private String estatus;
    private SaldoResponse saldo;
}