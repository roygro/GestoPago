package com.proyecto.servicios.exception;

public class ProductoIntegrationException extends RuntimeException {

    public ProductoIntegrationException(String message) {
        super(message);
    }

    public ProductoIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}