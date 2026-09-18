package com.proyecto.servicios.exception;

public class ProductoEmptyResponseException extends ProductoIntegrationException {

    public ProductoEmptyResponseException(String message) {
        super(message);
    }

    public ProductoEmptyResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}