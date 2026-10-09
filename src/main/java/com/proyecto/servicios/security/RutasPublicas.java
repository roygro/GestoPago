package com.proyecto.servicios.security;

import org.springframework.util.AntPathMatcher;

import java.util.List;

/**
 * Única fuente de verdad de los endpoints que NO requieren sesión.
 * La usan el filtro de sesión y la documentación de Swagger (para saber a qué
 * endpoints ponerles el candado y la respuesta 401).
 */
public final class RutasPublicas {

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    private static final List<String> PATRONES = List.of(
            "/auth/login",
            "/auth/credenciales",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/actuator/health",
            "/actuator/health/**",
            "/error"
    );

    private RutasPublicas() {
    }

    public static boolean esPublica(String metodo, String ruta) {
        // El registro de un cliente nuevo es público: todavía no tiene sesión.
        if ("POST".equalsIgnoreCase(metodo) && "/clientes".equals(ruta)) {
            return true;
        }
        // Los catálogos (solo lectura) son públicos: el formulario de registro los necesita antes de tener sesión.
        if ("GET".equalsIgnoreCase(metodo) && MATCHER.match("/catalogos/**", ruta)) {
            return true;
        }
        return PATRONES.stream().anyMatch(patron -> MATCHER.match(patron, ruta));
    }
}
