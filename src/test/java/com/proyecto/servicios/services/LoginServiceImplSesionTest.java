package com.proyecto.servicios.services;

import com.proyecto.servicios.entity.sf.Cliente;
import com.proyecto.servicios.entity.sf.Login;
import com.proyecto.servicios.exception.cliente.ValidacionNegocioException;
import com.proyecto.servicios.model.cliente.LoginRequest;
import com.proyecto.servicios.model.cliente.LoginResponse;
import com.proyecto.servicios.repositorys.sf.ClienteRepository;
import com.proyecto.servicios.repositorys.sf.LoginRepository;
import com.proyecto.servicios.security.EstadoSesion;
import com.proyecto.servicios.security.JwtUtil;
import com.proyecto.servicios.service.Impl.LoginServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas de la bandera de sesión y de los 5 minutos de inactividad.
 * El tiempo se controla con un Clock fijo: no hay que esperar 5 minutos reales.
 */
class LoginServiceImplSesionTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 7, 12, 0, 0);

    private LoginRepository loginRepository;
    private PasswordEncoder passwordEncoder;
    private JwtUtil jwtUtil;
    private LoginServiceImpl service;

    @BeforeEach
    void setUp() {
        loginRepository = mock(LoginRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtUtil = mock(JwtUtil.class);

        ZoneId zona = ZoneId.systemDefault();
        Clock reloj = Clock.fixed(AHORA.atZone(zona).toInstant(), zona);

        service = new LoginServiceImpl();
        ReflectionTestUtils.setField(service, "loginRepository", loginRepository);
        ReflectionTestUtils.setField(service, "clienteRepository", mock(ClienteRepository.class));
        ReflectionTestUtils.setField(service, "passwordEncoder", passwordEncoder);
        ReflectionTestUtils.setField(service, "jwtUtil", jwtUtil);
        ReflectionTestUtils.setField(service, "clock", reloj);
        ReflectionTestUtils.setField(service, "minutosInactividad", 5);
    }

    private Cliente cliente(boolean activo) {
        Cliente c = new Cliente();
        c.setId(1L);
        c.setActivo(activo);
        return c;
    }

    private Login login(LocalDateTime ultimaActividad, boolean sesionActiva, boolean clienteActivo) {
        Login l = new Login();
        l.setCliente(cliente(clienteActivo));
        l.setJwtToken("tok");
        l.setSesionActiva(sesionActiva);
        l.setUltimaActividad(ultimaActividad);
        l.setMinutosExpiracionInactividad(5);
        return l;
    }

    @Test
    void sesionDentroDeLosCincoMinutos_esValidaYRenuevaUltimaActividad() {
        Login l = login(AHORA.minusMinutes(2), true, true);
        when(loginRepository.findByJwtToken("tok")).thenReturn(Optional.of(l));

        assertEquals(EstadoSesion.VALIDA, service.validarSesion("tok"));

        assertTrue(l.getSesionActiva());
        assertEquals(AHORA, l.getUltimaActividad());
        verify(loginRepository).save(l);
    }

    @Test
    void sesionConExactamenteCincoMinutos_sigueValida() {
        Login l = login(AHORA.minusMinutes(5), true, true);
        when(loginRepository.findByJwtToken("tok")).thenReturn(Optional.of(l));

        assertEquals(EstadoSesion.VALIDA, service.validarSesion("tok"));
        assertTrue(l.getSesionActiva());
    }

    @Test
    void sesionConMasDeCincoMinutos_seApagaLaBandera_yDiceInactividad() {
        Login l = login(AHORA.minusMinutes(5).minusSeconds(1), true, true);
        when(loginRepository.findByJwtToken("tok")).thenReturn(Optional.of(l));

        assertEquals(EstadoSesion.INACTIVIDAD, service.validarSesion("tok"));

        assertFalse(l.getSesionActiva());
        verify(loginRepository).save(l);
    }

    @Test
    void sesionCerradaRecientemente_diceCerrada_yNoSeRenueva() {
        Login l = login(AHORA.minusMinutes(1), false, true);
        when(loginRepository.findByJwtToken("tok")).thenReturn(Optional.of(l));

        assertEquals(EstadoSesion.CERRADA, service.validarSesion("tok"));

        assertEquals(AHORA.minusMinutes(1), l.getUltimaActividad());
        verify(loginRepository, never()).save(any(Login.class));
    }

    @Test
    void sesionYaApagadaYConMasDeCincoMinutos_diceInactividad() {
        // Caso típico: el scheduler ya apagó la bandera y el usuario vuelve a llamar.
        Login l = login(AHORA.minusMinutes(10), false, true);
        when(loginRepository.findByJwtToken("tok")).thenReturn(Optional.of(l));

        assertEquals(EstadoSesion.INACTIVIDAD, service.validarSesion("tok"));
    }

    @Test
    void tokenDesconocido_diceNoEncontradaSinLanzarExcepcion() {
        when(loginRepository.findByJwtToken("nada")).thenReturn(Optional.empty());

        assertEquals(EstadoSesion.NO_ENCONTRADA, service.validarSesion("nada"));
    }

    @Test
    void clienteDadoDeBaja_cierraLaSesionAunqueNoHayaExpirado() {
        Login l = login(AHORA.minusMinutes(1), true, false);
        when(loginRepository.findByJwtToken("tok")).thenReturn(Optional.of(l));

        assertEquals(EstadoSesion.CLIENTE_INACTIVO, service.validarSesion("tok"));

        assertFalse(l.getSesionActiva());
    }

    @Test
    void cerrarSesionesVencidas_usaLaHoraDelReloj() {
        when(loginRepository.cerrarSesionesVencidas(AHORA)).thenReturn(3);

        assertEquals(3, service.cerrarSesionesVencidas());
    }

    // ---------- mensajes exactos del login ----------

    private LoginRequest request(String usuario, String password) {
        LoginRequest req = new LoginRequest();
        req.setUsuario(usuario);
        req.setPassword(password);
        return req;
    }

    @Test
    void login_usuarioInexistente_diceQueElUsuarioNoExiste() {
        when(loginRepository.findTopByUsuarioOrderByFechaInicioSesionDesc("nadie")).thenReturn(Optional.empty());

        ValidacionNegocioException ex = assertThrows(ValidacionNegocioException.class,
                () -> service.autenticar(request("nadie", "x")));

        assertEquals("El usuario no existe", ex.getMessage());
    }

    @Test
    void login_passwordIncorrecta_diceQueLaContrasenaEsIncorrecta() {
        Login previo = login(AHORA, true, true);
        previo.setPasswordHash("hash");
        when(loginRepository.findTopByUsuarioOrderByFechaInicioSesionDesc("juan")).thenReturn(Optional.of(previo));
        when(passwordEncoder.matches("mala", "hash")).thenReturn(false);

        ValidacionNegocioException ex = assertThrows(ValidacionNegocioException.class,
                () -> service.autenticar(request("juan", "mala")));

        assertEquals("La contraseña es incorrecta", ex.getMessage());
        verify(loginRepository, never()).save(any(Login.class));
    }

    @Test
    void login_clienteInactivoConPasswordCorrecta_diceQueElClienteEstaInactivo() {
        Login previo = login(AHORA, false, false);
        previo.setPasswordHash("hash");
        when(loginRepository.findTopByUsuarioOrderByFechaInicioSesionDesc("juan")).thenReturn(Optional.of(previo));
        when(passwordEncoder.matches("buena", "hash")).thenReturn(true);

        ValidacionNegocioException ex = assertThrows(ValidacionNegocioException.class,
                () -> service.autenticar(request("juan", "buena")));

        assertEquals("El cliente está inactivo, no puede iniciar sesión", ex.getMessage());
    }

    @Test
    void login_clienteInactivoConPasswordIncorrecta_noRevelaQueEstaInactivo() {
        Login previo = login(AHORA, false, false);
        previo.setPasswordHash("hash");
        when(loginRepository.findTopByUsuarioOrderByFechaInicioSesionDesc("juan")).thenReturn(Optional.of(previo));
        when(passwordEncoder.matches("mala", "hash")).thenReturn(false);

        ValidacionNegocioException ex = assertThrows(ValidacionNegocioException.class,
                () -> service.autenticar(request("juan", "mala")));

        assertEquals("La contraseña es incorrecta", ex.getMessage());
    }

    @Test
    void login_creaSesionActivaConLosMinutosConfiguradosYCierraLaAnterior() {
        ReflectionTestUtils.setField(service, "minutosInactividad", 3);

        Login previo = login(AHORA.minusMinutes(1), true, true);
        previo.setPasswordHash("hash");
        when(loginRepository.findTopByUsuarioOrderByFechaInicioSesionDesc("juan")).thenReturn(Optional.of(previo));
        when(passwordEncoder.matches("secreta", "hash")).thenReturn(true);
        when(loginRepository.findByClienteIdAndSesionActivaTrue(1L)).thenReturn(List.of(previo));
        when(jwtUtil.generarToken(1L, "juan")).thenReturn("nuevo");
        when(jwtUtil.obtenerFechaExpiracion("nuevo")).thenReturn(AHORA.plusHours(8));

        LoginRequest req = new LoginRequest();
        req.setUsuario("juan");
        req.setPassword("secreta");

        LoginResponse resp = service.autenticar(req);

        assertTrue(resp.getSesionActiva());
        assertEquals(3, resp.getMinutosExpiracionInactividad());
        assertFalse(previo.getSesionActiva(), "la sesión anterior debe quedar en false");

        ArgumentCaptor<Login> captor = ArgumentCaptor.forClass(Login.class);
        verify(loginRepository).save(captor.capture());
        assertTrue(captor.getValue().getSesionActiva());
        assertEquals(3, captor.getValue().getMinutosExpiracionInactividad());
    }
}
