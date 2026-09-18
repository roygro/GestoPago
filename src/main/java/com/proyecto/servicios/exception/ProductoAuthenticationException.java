package com.proyecto.servicios.exception;

public class ProductoAuthenticationException extends ProductoIntegrationException {

    public ProductoAuthenticationException(String message) {
        super(message);
    }

    public ProductoAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}