package com.proyecto.servicios.services;

import com.proyecto.servicios.entity.sf.Cliente;
import com.proyecto.servicios.entity.sf.Cuenta;
import com.proyecto.servicios.exception.cliente.ValidacionNegocioException;
import com.proyecto.servicios.model.pago.Solicitud2faResponse;
import com.proyecto.servicios.repositorys.sf.CuentaRepository;
import com.proyecto.servicios.service.Impl.TwoFactorServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TwoFactorServiceImplTest {

    private CuentaRepository cuentaRepository;
    private JavaMailSender mailSender;
    private TwoFactorServiceImpl twoFactorService;

    @BeforeEach
    void setUp() {
        cuentaRepository = mock(CuentaRepository.class);
        mailSender = mock(JavaMailSender.class);
        twoFactorService = new TwoFactorServiceImpl(cuentaRepository, mailSender);
        org.springframework.test.util.ReflectionTestUtils.setField(twoFactorService, "vigenciaMinutos", 5);
    }

    private Cuenta crearCuenta(String numCuenta, String correo, boolean activa, boolean clienteActivo) {
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNombre("Ariana");
        cliente.setCorreoElectronico(correo);
        cliente.setActivo(clienteActivo);

        Cuenta cuenta = new Cuenta();
        cuenta.setId(2L);
        cuenta.setNumeroCuenta(numCuenta);
        cuenta.setEstatus(activa ? "ACTIVA" : "INACTIVA");
        cuenta.setCliente(cliente);

        return cuenta;
    }

    @Test
    @DisplayName("Generar código 2FA exitoso con correo ofuscado")
    void generarCodigoExitoso() {
        String numCuenta = "7898384477103410";
        Cuenta cuenta = crearCuenta(numCuenta, "ariana.perez@empresademo.com", true, true);
        when(cuentaRepository.findByNumeroCuenta(numCuenta)).thenReturn(Optional.of(cuenta));

        Solicitud2faResponse respuesta = twoFactorService.generarYEnviarCodigo(numCuenta);

        assertNotNull(respuesta);
        assertEquals(numCuenta, respuesta.getNumeroCuenta());
        assertTrue(respuesta.getCorreoDestino().contains("*****"));
        assertEquals(5, respuesta.getVigenciaMinutos());
    }

    @Test
    @DisplayName("Validar código correcto y luego consumirlo para que sea de un solo uso")
    void validarYConsumirCodigo() {
        String numCuenta = "7898384477103410";
        Cuenta cuenta = crearCuenta(numCuenta, "ariana.perez@empresademo.com", true, true);
        when(cuentaRepository.findByNumeroCuenta(numCuenta)).thenReturn(Optional.of(cuenta));

        twoFactorService.generarYEnviarCodigo(numCuenta);

        // Código maestro comodín funciona siempre
        assertDoesNotThrow(() -> twoFactorService.validarCodigo(numCuenta, "123456"));

        // Al consumir el código, ya no se puede reutilizar
        twoFactorService.consumirCodigo(numCuenta);

        // Sin código activo y con código no maestro, debe fallar
        assertThrows(ValidacionNegocioException.class,
                () -> twoFactorService.validarCodigo(numCuenta, "999999"));
    }

    @Test
    @DisplayName("Código 2FA incorrecto reduce intentos y lanza ValidacionNegocioException")
    void codigoIncorrectoFalla() {
        String numCuenta = "7898384477103410";
        Cuenta cuenta = crearCuenta(numCuenta, "ariana.perez@empresademo.com", true, true);
        when(cuentaRepository.findByNumeroCuenta(numCuenta)).thenReturn(Optional.of(cuenta));

        twoFactorService.generarYEnviarCodigo(numCuenta);

        ValidacionNegocioException ex = assertThrows(ValidacionNegocioException.class,
                () -> twoFactorService.validarCodigo(numCuenta, "000000"));

        assertTrue(ex.getMessage().contains("incorrecto"));
    }
}
