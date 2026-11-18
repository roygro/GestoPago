package com.proyecto.servicios.services;

import com.proyecto.servicios.exception.cliente.AccesoDenegadoException;
import com.proyecto.servicios.security.SesionLimpiezaScheduler;
import com.proyecto.servicios.security.UsuarioAutenticado;
import com.proyecto.servicios.service.LoginService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UsuarioAutenticadoTest {

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void obtenerClienteId_exito() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("clienteId", 42L);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        UsuarioAutenticado auth = new UsuarioAutenticado();
        assertEquals(42L, auth.obtenerClienteId());
        assertDoesNotThrow(() -> auth.validarPropietario(42L));
    }

    @Test
    void obtenerClienteId_sinAtributos_lanzaAccesoDenegado() {
        UsuarioAutenticado auth = new UsuarioAutenticado();
        assertThrows(AccesoDenegadoException.class, auth::obtenerClienteId);
    }

    @Test
    void validarPropietario_clienteDistinto_lanzaAccesoDenegado() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("clienteId", 42L);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        UsuarioAutenticado auth = new UsuarioAutenticado();
        assertThrows(AccesoDenegadoException.class, () -> auth.validarPropietario(99L));
    }

    @Test
    void sesionLimpiezaScheduler_cierraSesiones() {
        LoginService loginService = mock(LoginService.class);
        when(loginService.cerrarSesionesVencidas()).thenReturn(5);

        SesionLimpiezaScheduler scheduler = new SesionLimpiezaScheduler(loginService);
        assertDoesNotThrow(scheduler::cerrarSesionesVencidas);
        verify(loginService).cerrarSesionesVencidas();
    }

    @Test
    void sesionLimpiezaScheduler_conError_noLanzaExcepcion() {
        LoginService loginService = mock(LoginService.class);
        when(loginService.cerrarSesionesVencidas()).thenThrow(new RuntimeException("DB caida"));

        SesionLimpiezaScheduler scheduler = new SesionLimpiezaScheduler(loginService);
        assertDoesNotThrow(scheduler::cerrarSesionesVencidas);
    }
}
