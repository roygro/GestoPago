package com.proyecto.servicios.exception.cliente;

public class CuentaNoEncontradaException extends ClienteBusinessException {
    public CuentaNoEncontradaException(String numeroCuenta) {
        super("Cuenta no encontrada: " + numeroCuenta);
    }
}