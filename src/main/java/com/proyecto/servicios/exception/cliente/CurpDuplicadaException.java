package com.proyecto.servicios.exception.cliente;

public class CurpDuplicadaException extends ClienteBusinessException {
    public CurpDuplicadaException(String curp) {
        super("Ya existe un cliente registrado con la CURP: " + curp);
    }
}