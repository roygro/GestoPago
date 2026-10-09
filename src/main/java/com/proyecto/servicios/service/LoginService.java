package com.proyecto.servicios.service;

import com.proyecto.servicios.model.cliente.LoginRequest;
import com.proyecto.servicios.model.cliente.LoginResponse;
import com.proyecto.servicios.security.EstadoSesion;

public interface LoginService {
    LoginResponse crearCredenciales(Long clienteId, String usuario, String passwordPlano);
    LoginResponse autenticar(LoginRequest request);
    void cerrarSesion(String jwtToken);

    /**
     * Valida la sesión asociada al token y dice exactamente en qué estado está.
     * Si es VALIDA, además renueva ultima_actividad. Nunca lanza excepción por token desconocido.
     */
    EstadoSesion validarSesion(String jwtToken);

    /** Apaga la bandera de las sesiones vencidas. Regresa cuántas cerró. */
    int cerrarSesionesVencidas();
}
