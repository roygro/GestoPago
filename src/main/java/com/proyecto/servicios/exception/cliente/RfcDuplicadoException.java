package com.proyecto.servicios.exception.cliente;

public class RfcDuplicadoException extends ClienteBusinessException {
    public RfcDuplicadoException(String rfc) {
        super("Ya existe un cliente registrado con el RFC: " + rfc);
    }
}