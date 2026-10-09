package com.proyecto.servicios.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.service.LoginService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Protege los endpoints con el JWT + la bandera de sesión (sesion_activa).
 *
 * Por cada petición protegida:
 *  1. Exige el encabezado Authorization: Bearer &lt;jwt&gt;.
 *  2. Valida firma y expiración del JWT.
 *  3. Valida en BD que la bandera sesion_activa sea true y que no hayan pasado
 *     los minutos de inactividad; si todo está bien, renueva ultima_actividad.
 *  Cualquier falla responde 401 con el mismo formato del resto de la API
 *  { "codigo": 401, "mensaje": "..." } y un mensaje que dice exactamente la causa.
 */
@Component
@Slf4j
public class SesionFilter extends OncePerRequestFilter {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String PREFIJO_BEARER = "Bearer ";

    private final JwtUtil jwtUtil;
    private final LoginService loginService;

    public SesionFilter(JwtUtil jwtUtil, LoginService loginService) {
        this.jwtUtil = jwtUtil;
        this.loginService = loginService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Preflight de CORS
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String uri = request.getRequestURI() == null ? "" : request.getRequestURI();
        // Defensa contra trucos tipo /swagger-ui/../clientes: ante la duda, SÍ se filtra.
        if (uri.contains("..") || uri.contains(";")) {
            return false;
        }

        // Se usa la ruta normalizada por el contenedor, no la URI cruda.
        String ruta = request.getServletPath();
        if (ruta == null || ruta.isEmpty()) {
            ruta = uri.substring(request.getContextPath().length());
        }
        return RutasPublicas.esPublica(request.getMethod(), ruta);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String encabezado = request.getHeader("Authorization");
        if (encabezado == null || !encabezado.regionMatches(true, 0, PREFIJO_BEARER, 0, PREFIJO_BEARER.length())) {
            responderNoAutorizado(response, "Se requiere un token de autenticación (Authorization: Bearer ...)");
            return;
        }
        String token = encabezado.substring(PREFIJO_BEARER.length()).trim();

        Claims claims;
        try {
            claims = jwtUtil.parseClaims(token);
        } catch (ExpiredJwtException e) {
            responderNoAutorizado(response, "El token ha expirado. Inicie sesión nuevamente");
            return;
        } catch (JwtException | IllegalArgumentException e) {
            responderNoAutorizado(response, "El token es inválido");
            return;
        }

        EstadoSesion estado = loginService.validarSesion(token);
        if (estado != EstadoSesion.VALIDA) {
            responderNoAutorizado(response, estado.getMensaje());
            return;
        }

        Object clienteId = claims.get("clienteId");
        if (clienteId instanceof Number) {
            request.setAttribute("clienteId", ((Number) clienteId).longValue());
        }
        chain.doFilter(request, response);
    }

    private void responderNoAutorizado(HttpServletResponse response, String mensaje) throws IOException {
        GenericResponse cuerpo = new GenericResponse();
        cuerpo.setCodigo(HttpServletResponse.SC_UNAUTHORIZED);
        cuerpo.setMensaje(mensaje);

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader("WWW-Authenticate", "Bearer");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        MAPPER.writeValue(response.getWriter(), cuerpo);
    }
}
