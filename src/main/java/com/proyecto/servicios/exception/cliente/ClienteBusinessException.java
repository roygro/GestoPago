package com.proyecto.servicios.exception.cliente;

public abstract class ClienteBusinessException extends RuntimeException {
    public ClienteBusinessException(String message) {
        super(message);
    }
}