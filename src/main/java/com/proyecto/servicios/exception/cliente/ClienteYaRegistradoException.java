package com.proyecto.servicios.exception.cliente;

public class ClienteYaRegistradoException extends ClienteBusinessException {
    public ClienteYaRegistradoException(String message) {
        super(message);
    }
}