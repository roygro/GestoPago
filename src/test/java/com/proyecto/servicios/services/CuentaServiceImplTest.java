package com.proyecto.servicios.services;

import com.proyecto.servicios.entity.sf.Cliente;
import com.proyecto.servicios.entity.sf.Cuenta;
import com.proyecto.servicios.entity.sf.Saldo;
import com.proyecto.servicios.exception.cliente.CuentaNoEncontradaException;
import com.proyecto.servicios.model.cliente.CuentaResponse;
import com.proyecto.servicios.model.cliente.SaldoResponse;
import com.proyecto.servicios.repositorys.sf.CuentaRepository;
import com.proyecto.servicios.security.UsuarioAutenticado;
import com.proyecto.servicios.service.Impl.CuentaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CuentaServiceImplTest {

    private CuentaRepository cuentaRepository;
    private UsuarioAutenticado usuarioAutenticado;
    private CuentaServiceImpl service;

    @BeforeEach
    void setUp() {
        cuentaRepository = mock(CuentaRepository.class);
        usuarioAutenticado = mock(UsuarioAutenticado.class);
        service = new CuentaServiceImpl();
        ReflectionTestUtils.setField(service, "cuentaRepository", cuentaRepository);
        ReflectionTestUtils.setField(service, "usuarioAutenticado", usuarioAutenticado);
    }

    @Test
    @DisplayName("consultarPorNumeroCuenta retorna DTO cuando existe y es propietario")
    void consultarPorNumeroCuenta_exito() {
        Cliente cliente = new Cliente();
        cliente.setId(10L);

        Cuenta cuenta = new Cuenta();
        cuenta.setId(1L);
        cuenta.setNumeroCuenta("1234567890123456");
        cuenta.setEstatus("ACTIVA");
        cuenta.setFechaApertura(LocalDateTime.now());
        cuenta.setCliente(cliente);

        Saldo saldo = new Saldo();
        saldo.setSaldoDisponible(new BigDecimal("500.00"));
        saldo.setFechaActualizacion(LocalDateTime.now());
        cuenta.setSaldo(saldo);

        when(cuentaRepository.findByNumeroCuenta("1234567890123456")).thenReturn(Optional.of(cuenta));

        CuentaResponse response = service.consultarPorNumeroCuenta("1234567890123456");

        assertNotNull(response);
        assertEquals("1234567890123456", response.getNumeroCuenta());
        assertEquals("ACTIVA", response.getEstatus());
        assertNotNull(response.getSaldo());
        assertEquals(new BigDecimal("500.00"), response.getSaldo().getSaldoDisponible());
        verify(usuarioAutenticado).validarPropietario(10L);
    }

    @Test
    @DisplayName("consultarPorNumeroCuenta lanza CuentaNoEncontradaException si no existe")
    void consultarPorNumeroCuenta_noExiste() {
        when(cuentaRepository.findByNumeroCuenta("0000000000000000")).thenReturn(Optional.empty());

        assertThrows(CuentaNoEncontradaException.class, () ->
                service.consultarPorNumeroCuenta("0000000000000000"));
    }

    @Test
    @DisplayName("consultarSaldo retorna saldo y fecha correctamente")
    void consultarSaldo_conSaldo() {
        Cliente cliente = new Cliente();
        cliente.setId(5L);

        Cuenta cuenta = new Cuenta();
        cuenta.setNumeroCuenta("1111222233334444");
        cuenta.setCliente(cliente);

        Saldo saldo = new Saldo();
        saldo.setSaldoDisponible(new BigDecimal("1250.75"));
        LocalDateTime fecha = LocalDateTime.now();
        saldo.setFechaActualizacion(fecha);
        cuenta.setSaldo(saldo);

        when(cuentaRepository.findByNumeroCuenta("1111222233334444")).thenReturn(Optional.of(cuenta));

        SaldoResponse response = service.consultarSaldo("1111222233334444");

        assertNotNull(response);
        assertEquals(new BigDecimal("1250.75"), response.getSaldoDisponible());
        assertEquals(fecha, response.getFechaActualizacion());
        verify(usuarioAutenticado).validarPropietario(5L);
    }

    @Test
    @DisplayName("consultarSaldo retorna DTO vacío si la cuenta no tiene saldo asociado")
    void consultarSaldo_sinSaldo() {
        Cliente cliente = new Cliente();
        cliente.setId(5L);

        Cuenta cuenta = new Cuenta();
        cuenta.setNumeroCuenta("1111222233334444");
        cuenta.setCliente(cliente);
        cuenta.setSaldo(null);

        when(cuentaRepository.findByNumeroCuenta("1111222233334444")).thenReturn(Optional.of(cuenta));

        SaldoResponse response = service.consultarSaldo("1111222233334444");

        assertNotNull(response);
        assertNull(response.getSaldoDisponible());
    }

    @Test
    @DisplayName("consultarActivas filtra solo cuentas con estatus ACTIVA del cliente autenticado")
    void consultarActivas_filtraSoloActivas() {
        when(usuarioAutenticado.obtenerClienteId()).thenReturn(7L);

        Cuenta c1 = new Cuenta();
        c1.setId(1L);
        c1.setNumeroCuenta("1111111111111111");
        c1.setEstatus("ACTIVA");

        Cuenta c2 = new Cuenta();
        c2.setId(2L);
        c2.setNumeroCuenta("2222222222222222");
        c2.setEstatus("INACTIVA");

        Cuenta c3 = new Cuenta();
        c3.setId(3L);
        c3.setNumeroCuenta("3333333333333333");
        c3.setEstatus("ACTIVA");

        when(cuentaRepository.findByClienteId(7L)).thenReturn(Arrays.asList(c1, c2, c3));

        List<CuentaResponse> activas = service.consultarActivas();

        assertEquals(2, activas.size());
        assertEquals("1111111111111111", activas.get(0).getNumeroCuenta());
        assertEquals("3333333333333333", activas.get(1).getNumeroCuenta());
    }
}
