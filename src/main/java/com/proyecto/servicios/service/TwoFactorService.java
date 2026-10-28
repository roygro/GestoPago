package com.proyecto.servicios.service;

import com.proyecto.servicios.model.pago.Solicitud2faResponse;

public interface TwoFactorService {

    /**
     * Genera un código OTP de 6 dígitos con vigencia temporal y lo envía al correo
     * registrado del titular de la cuenta. También lo registra en log para pruebas locales.
     */
    Solicitud2faResponse generarYEnviarCodigo(String numeroCuenta);

    /**
     * Valida que el código ingresado coincida con el generado para la cuenta y no haya expirado.
     */
    void validarCodigo(String numeroCuenta, String codigoIngresado);

    /**
     * Elimina el código una vez utilizado con éxito para garantizar un solo uso.
     */
    void consumirCodigo(String numeroCuenta);
}
