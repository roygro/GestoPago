package com.proyecto.servicios.exception;

public class ProductoTimeoutException extends ProductoIntegrationException {

    public ProductoTimeoutException(String message) {
        super(message);
    }

    public ProductoTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}