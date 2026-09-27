package com.proyecto.servicios.service;

import com.proyecto.servicios.model.cliente.LoginRequest;
import com.proyecto.servicios.model.cliente.LoginResponse;

public interface LoginService {
    LoginResponse crearCredenciales(Long clienteId, String usuario, String passwordPlano);
    LoginResponse autenticar(LoginRequest request);
    void cerrarSesion(String jwtToken);
    boolean validarSesionActiva(String jwtToken);
}