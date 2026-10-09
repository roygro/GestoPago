package com.proyecto.servicios.exception.cliente;

public class ClienteNoEncontradoException extends ClienteBusinessException {
    public ClienteNoEncontradoException(String detalle) {
        super("Cliente no encontrado: " + detalle);
    }
}