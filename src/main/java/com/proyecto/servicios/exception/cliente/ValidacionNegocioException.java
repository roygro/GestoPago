package com.proyecto.servicios.exception.cliente;

public class ValidacionNegocioException extends ClienteBusinessException {
    public ValidacionNegocioException(String message) {
        super(message);
    }
}