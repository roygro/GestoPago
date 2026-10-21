package com.proyecto.servicios.exception.cliente;

public class AccesoDenegadoException extends ClienteBusinessException {
    public AccesoDenegadoException(String message) {
        super(message);
    }
}