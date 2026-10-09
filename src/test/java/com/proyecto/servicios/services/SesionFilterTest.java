package com.proyecto.servicios.services;

import com.proyecto.servicios.security.EstadoSesion;
import com.proyecto.servicios.security.JwtUtil;
import com.proyecto.servicios.security.SesionFilter;
import com.proyecto.servicios.service.LoginService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SesionFilterTest {

    private static final String SECRETO = Base64.getEncoder()
            .encodeToString("clave-de-prueba-0123456789-abcdefghijklmnopqrstuvwxyz".getBytes(StandardCharsets.UTF_8));
    private static final String OTRO_SECRETO = Base64.getEncoder()
            .encodeToString("otra-clave-distinta-9876543210-zyxwvutsrqponmlkjihgfedcba".getBytes(StandardCharsets.UTF_8));

    private JwtUtil jwtUtil;
    private LoginService loginService;
    private SesionFilter filtro;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRETO, 8);
        loginService = mock(LoginService.class);
        filtro = new SesionFilter(jwtUtil, loginService);
    }

    private MockHttpServletRequest peticion(String metodo, String ruta) {
        MockHttpServletRequest r = new MockHttpServletRequest(metodo, ruta);
        r.setServletPath(ruta);
        return r;
    }

    private void assertNoAutorizado(MockHttpServletResponse res, MockFilterChain cadena) throws Exception {
        assertNoAutorizado(res, cadena, null);
    }

    private void assertNoAutorizado(MockHttpServletResponse res, MockFilterChain cadena, String mensajeEsperado) throws Exception {
        assertEquals(401, res.getStatus());
        assertNull(cadena.getRequest(), "no debe continuar a la API");
        assertTrue(res.getContentType().startsWith("application/json"));
        String cuerpo = res.getContentAsString();
        assertTrue(cuerpo.contains("\"codigo\":401"));
        assertTrue(cuerpo.contains("\"mensaje\""));
        assertFalse(cuerpo.contains("timestamp"), "la respuesta solo lleva codigo y mensaje");
        if (mensajeEsperado != null) {
            assertTrue(cuerpo.contains(mensajeEsperado), "mensaje esperado: " + mensajeEsperado + " | real: " + cuerpo);
        }
    }

    @Test
    void sinEncabezadoAuthorization_responde401() throws Exception {
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain cadena = new MockFilterChain();

        filtro.doFilter(peticion("GET", "/clientes"), res, cadena);

        assertNoAutorizado(res, cadena);
    }

    @Test
    void esquemaDistintoDeBearer_responde401() throws Exception {
        MockHttpServletRequest req = peticion("GET", "/clientes");
        req.addHeader("Authorization", "Basic YWJjOmRlZg==");
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain cadena = new MockFilterChain();

        filtro.doFilter(req, res, cadena);

        assertNoAutorizado(res, cadena);
    }

    @Test
    void tokenBasura_responde401() throws Exception {
        MockHttpServletRequest req = peticion("GET", "/clientes");
        req.addHeader("Authorization", "Bearer esto.no.es-un-jwt");
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain cadena = new MockFilterChain();

        filtro.doFilter(req, res, cadena);

        assertNoAutorizado(res, cadena, "El token es inválido");
        verifyNoInteractions(loginService);
    }

    @Test
    void tokenExpirado_diceQueElTokenHaExpirado() throws Exception {
        String vencido = new JwtUtil(SECRETO, -1).generarToken(1L, "juan");
        MockHttpServletRequest req = peticion("GET", "/clientes");
        req.addHeader("Authorization", "Bearer " + vencido);
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain cadena = new MockFilterChain();

        filtro.doFilter(req, res, cadena);

        assertNoAutorizado(res, cadena, "El token ha expirado");
        verifyNoInteractions(loginService);
    }

    @Test
    void tokenFirmadoConOtraClave_responde401() throws Exception {
        String falso = new JwtUtil(OTRO_SECRETO, 8).generarToken(1L, "hacker");
        MockHttpServletRequest req = peticion("GET", "/clientes");
        req.addHeader("Authorization", "Bearer " + falso);
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain cadena = new MockFilterChain();

        filtro.doFilter(req, res, cadena);

        assertNoAutorizado(res, cadena, "El token es inválido");
        verifyNoInteractions(loginService);
    }

    @Test
    void sinEncabezado_diceQueSeRequiereToken() throws Exception {
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain cadena = new MockFilterChain();

        filtro.doFilter(peticion("GET", "/clientes"), res, cadena);

        assertNoAutorizado(res, cadena, "Se requiere un token de autenticación");
    }

    @Test
    void cadaEstadoDeSesionInvalidaResponde401ConSuPropioMensaje() throws Exception {
        for (EstadoSesion estado : EstadoSesion.values()) {
            if (estado == EstadoSesion.VALIDA) {
                continue;
            }
            String token = jwtUtil.generarToken(1L, "juan");
            when(loginService.validarSesion(token)).thenReturn(estado);
            MockHttpServletRequest req = peticion("GET", "/clientes");
            req.addHeader("Authorization", "Bearer " + token);
            MockHttpServletResponse res = new MockHttpServletResponse();
            MockFilterChain cadena = new MockFilterChain();

            filtro.doFilter(req, res, cadena);

            assertNoAutorizado(res, cadena, estado.getMensaje());
        }
    }

    @Test
    void tokenValidoYSesionActiva_dejaPasarYExponeElClienteId() throws Exception {
        String token = jwtUtil.generarToken(7L, "juan");
        when(loginService.validarSesion(token)).thenReturn(EstadoSesion.VALIDA);
        MockHttpServletRequest req = peticion("GET", "/clientes");
        req.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain cadena = new MockFilterChain();

        filtro.doFilter(req, res, cadena);

        assertEquals(200, res.getStatus());
        assertNotNull(cadena.getRequest(), "debe continuar a la API");
        assertEquals(7L, req.getAttribute("clienteId"));
    }

    @Test
    void rutasPublicas_pasanSinToken() throws Exception {
        String[][] publicas = {
                {"POST", "/clientes"},
                {"POST", "/auth/login"},
                {"POST", "/auth/credenciales"},
                {"GET", "/v3/api-docs"},
                {"GET", "/swagger-ui/index.html"},
                {"GET", "/catalogos/estado-civil"}
        };
        for (String[] p : publicas) {
            MockHttpServletResponse res = new MockHttpServletResponse();
            MockFilterChain cadena = new MockFilterChain();

            filtro.doFilter(peticion(p[0], p[1]), res, cadena);

            assertNotNull(cadena.getRequest(), p[0] + " " + p[1] + " debe ser público");
        }
    }

    @Test
    void getClientes_noEsPublico_soloElPostDeRegistro() throws Exception {
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain cadena = new MockFilterChain();

        filtro.doFilter(peticion("DELETE", "/clientes"), res, cadena);

        assertNoAutorizado(res, cadena);
    }

    @Test
    void catalogos_soloSonPublicosParaGET() throws Exception {
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain cadena = new MockFilterChain();

        filtro.doFilter(peticion("POST", "/catalogos/estado-civil"), res, cadena);

        assertNoAutorizado(res, cadena);
    }

    @Test
    void truco_swaggerUiPuntoPunto_noSaltaLaProteccion() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/swagger-ui/../clientes");
        req.setServletPath("/clientes");
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain cadena = new MockFilterChain();

        filtro.doFilter(req, res, cadena);

        assertNoAutorizado(res, cadena);
    }
}
