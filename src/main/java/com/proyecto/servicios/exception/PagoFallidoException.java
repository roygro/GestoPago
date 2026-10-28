package com.proyecto.servicios.exception;

import lombok.Getter;

@Getter
public class PagoFallidoException extends RuntimeException {

    private final String codigo;
    private final String idTx;

    public PagoFallidoException(String mensaje) {
        super(mensaje);
        this.codigo = null;
        this.idTx = null;
    }

    public PagoFallidoException(String codigo, String idTx, String mensaje) {
        super("Transacción rechazada por el proveedor [Código " + codigo + "]: " + mensaje);
        this.codigo = codigo;
        this.idTx = idTx;
    }
}
