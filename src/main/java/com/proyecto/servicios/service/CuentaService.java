package com.proyecto.servicios.service;

import com.proyecto.servicios.model.cliente.CuentaResponse;
import com.proyecto.servicios.model.cliente.SaldoResponse;

import java.util.List;

public interface CuentaService {
    CuentaResponse consultarPorNumeroCuenta(String numeroCuenta);
    SaldoResponse consultarSaldo(String numeroCuenta);
    List<CuentaResponse> consultarActivas();
}