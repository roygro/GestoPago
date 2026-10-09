package com.proyecto.servicios.security;

/**
 * Resultado de validar una sesión. Cada estado inválido lleva su propio mensaje,
 * para que el 401 diga exactamente qué pasó.
 */
public enum EstadoSesion {
    VALIDA(null),
    NO_ENCONTRADA("La sesión no existe. Inicie sesión nuevamente"),
    CERRADA("La sesión fue cerrada (cierre de sesión o nuevo inicio de sesión). Inicie sesión nuevamente"),
    INACTIVIDAD("La sesión expiró por inactividad. Inicie sesión nuevamente"),
    CLIENTE_INACTIVO("El cliente está inactivo, su sesión ya no es válida");

    private final String mensaje;

    EstadoSesion(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getMensaje() {
        return mensaje;
    }
}
