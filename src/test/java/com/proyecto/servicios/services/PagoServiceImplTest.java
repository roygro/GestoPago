package com.proyecto.servicios.services;

import com.proyecto.servicios.client.GestoPagoSendTxClient;
import com.proyecto.servicios.entity.pago.TransaccionPago;
import com.proyecto.servicios.entity.sf.Cliente;
import com.proyecto.servicios.entity.sf.Cuenta;
import com.proyecto.servicios.entity.sf.Saldo;
import com.proyecto.servicios.exception.PagoFallidoException;
import com.proyecto.servicios.exception.cliente.CuentaNoEncontradaException;
import com.proyecto.servicios.exception.cliente.ValidacionNegocioException;
import com.proyecto.servicios.model.pago.PagoRequest;
import com.proyecto.servicios.model.pago.PagoResponse;
import com.proyecto.servicios.model.producto.ProductoDTO;
import com.proyecto.servicios.model.producto.ProductoListResponse;
import com.proyecto.servicios.repositorys.pago.TransaccionPagoRepository;
import com.proyecto.servicios.repositorys.sf.CuentaRepository;
import com.proyecto.servicios.repositorys.sf.SaldoRepository;
import com.proyecto.servicios.security.UsuarioAutenticado;
import com.proyecto.servicios.service.Impl.PagoServiceImpl;
import com.proyecto.servicios.service.ProductoService;
import com.proyecto.servicios.service.TwoFactorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class PagoServiceImplTest {

    private CuentaRepository cuentaRepository;
    private SaldoRepository saldoRepository;
    private TransaccionPagoRepository transaccionPagoRepository;
    private GestoPagoSendTxClient gestoPagoSendTxClient;
    private UsuarioAutenticado usuarioAutenticado;
    private TwoFactorService twoFactorService;
    private ProductoService productoService;
    private PagoServiceImpl pagoService;

    private static final String SECRET_KEY = "HPo7OLqB4Fkk4E2yGOtwqw8H5fHR9kNx67OR4g4UdlA=";
    private static final String IV = "p5ldmBPdd/9pjC0bDC/nSg==";

    @BeforeEach
    void setUp() {
        cuentaRepository = mock(CuentaRepository.class);
        saldoRepository = mock(SaldoRepository.class);
        transaccionPagoRepository = mock(TransaccionPagoRepository.class);
        gestoPagoSendTxClient = mock(GestoPagoSendTxClient.class);
        usuarioAutenticado = mock(UsuarioAutenticado.class);
        twoFactorService = mock(TwoFactorService.class);
        productoService = mock(ProductoService.class);

        pagoService = new PagoServiceImpl(
                cuentaRepository,
                saldoRepository,
                transaccionPagoRepository,
                gestoPagoSendTxClient,
                usuarioAutenticado,
                twoFactorService,
                productoService
        );

        ReflectionTestUtils.setField(pagoService, "secretKey", SECRET_KEY);
        ReflectionTestUtils.setField(pagoService, "iv", IV);
        ReflectionTestUtils.setField(pagoService, "unidad", "083");

        // Configuración por defecto de catálogo para los tests
        ProductoDTO prod = new ProductoDTO();
        prod.setIdProducto(582);
        prod.setIdServicio(133);
        prod.setNombreProducto("Telcel 10");
        prod.setPrecio(new BigDecimal("10.00"));

        ProductoListResponse catRes = new ProductoListResponse();
        catRes.setProductos(Map.of("1", List.of(prod)));
        when(productoService.obtenerListaProductos()).thenReturn(catRes);
    }

    private Cuenta crearCuentaValida(String numeroCuenta, BigDecimal saldoMonto) {
        Cliente cliente = new Cliente();
        cliente.setId(10L);
        cliente.setNombre("Juan");
        cliente.setActivo(true);

        Cuenta cuenta = new Cuenta();
        cuenta.setId(1L);
        cuenta.setNumeroCuenta(numeroCuenta);
        cuenta.setEstatus("ACTIVA");
        cuenta.setCliente(cliente);

        Saldo saldo = new Saldo();
        saldo.setId(5L);
        saldo.setCuenta(cuenta);
        saldo.setSaldoDisponible(saldoMonto);
        saldo.setFechaActualizacion(LocalDateTime.now());
        cuenta.setSaldo(saldo);

        when(saldoRepository.findByCuenta_NumeroCuentaConBloqueo(numeroCuenta)).thenReturn(Optional.of(saldo));

        return cuenta;
    }

    @Test
    @DisplayName("Pago exitoso: código 01 debita el saldo y devuelve PagoResponse")
    void procesarPagoExitoso() {
        String numCuenta = "1234567890123456";
        Cuenta cuenta = crearCuentaValida(numCuenta, new BigDecimal("100.00"));
        when(cuentaRepository.findByNumeroCuenta(numCuenta)).thenReturn(Optional.of(cuenta));

        String xmlExitoso = "<?xml version='1.0' encoding='UTF-8'?>"
                + "<RESPONSE>"
                + "    <ID_TX>291437188</ID_TX>"
                + "    <NUM_AUTORIZACION>100024988</NUM_AUTORIZACION>"
                + "    <SALDO>157731.83</SALDO>"
                + "    <FECHA>23/Oct/2026 18:25:00</FECHA>"
                + "    <MONTO>10.0</MONTO>"
                + "    <MENSAJE>"
                + "        <CODIGO>01</CODIGO>"
                + "        <TEXTO>Operacion realizada con exito</TEXTO>"
                + "    </MENSAJE>"
                + "</RESPONSE>";

        when(gestoPagoSendTxClient.sendTx(anyString(), anyString())).thenReturn(xmlExitoso);

        PagoRequest request = PagoRequest.builder()
                .numCuenta(numCuenta)
                .idProd(582)
                .idSer(133)
                .monto(new BigDecimal("10.00"))
                .telefono("5555555551")
                .reference("REF123")
                .twoFactorCode("123456")
                .build();

        PagoResponse respuesta = pagoService.procesarPago(request);

        assertNotNull(respuesta);
        assertEquals("291437188", respuesta.getIdTx());
        assertEquals("100024988", respuesta.getNumAutorizacion());
        assertEquals("01", respuesta.getCodigoRespuesta());
        assertEquals(new BigDecimal("100.00"), respuesta.getSaldoAnterior());
        assertEquals(new BigDecimal("90.00"), respuesta.getNuevoSaldo());
        assertNotNull(respuesta.getUpc());
        assertEquals(10, respuesta.getUpc().length());

        verify(saldoRepository, times(1)).save(any(Saldo.class));
        verify(twoFactorService, times(1)).consumirCodigo(numCuenta);
        verify(transaccionPagoRepository, atLeastOnce()).save(any(TransaccionPago.class));
    }

    @Test
    @DisplayName("Doble clic o transacción duplicada reciente lanza ValidacionNegocioException")
    void transaccionDuplicadaRecienteLanzaExcepcion() {
        String numCuenta = "1234567890123456";
        when(transaccionPagoRepository.existeTransaccionRecienteDuplicada(
                eq(numCuenta), eq(133), eq(582), any(BigDecimal.class), any(), any(), any()))
                .thenReturn(true);

        PagoRequest request = PagoRequest.builder()
                .numCuenta(numCuenta)
                .idProd(582)
                .idSer(133)
                .monto(new BigDecimal("10.00"))
                .twoFactorCode("123456")
                .build();

        ValidacionNegocioException ex = assertThrows(ValidacionNegocioException.class,
                () -> pagoService.procesarPago(request));

        assertTrue(ex.getMessage().contains("cargo duplicado"));
        verify(gestoPagoSendTxClient, never()).sendTx(anyString(), anyString());
    }

    @Test
    @DisplayName("Monto manipulado o discrepante del precio de catálogo lanza ValidacionNegocioException")
    void montoManipuladoLanzaExcepcion() {
        String numCuenta = "1234567890123456";

        // Producto 582 tiene precio fijo 10.00, pero mandan 25.00
        PagoRequest request = PagoRequest.builder()
                .numCuenta(numCuenta)
                .idProd(582)
                .idSer(133)
                .monto(new BigDecimal("25.00"))
                .twoFactorCode("123456")
                .build();

        ValidacionNegocioException ex = assertThrows(ValidacionNegocioException.class,
                () -> pagoService.procesarPago(request));

        assertTrue(ex.getMessage().contains("no corresponde al precio fijo"));
        verify(gestoPagoSendTxClient, never()).sendTx(anyString(), anyString());
    }

    @Test
    @DisplayName("Cuenta inexistente lanza CuentaNoEncontradaException")
    void cuentaInexistenteLanzaExcepcion() {
        when(cuentaRepository.findByNumeroCuenta("9999999999999999")).thenReturn(Optional.empty());

        PagoRequest request = PagoRequest.builder()
                .numCuenta("9999999999999999")
                .idProd(582)
                .idSer(133)
                .monto(new BigDecimal("10.00"))
                .twoFactorCode("123456")
                .build();

        assertThrows(CuentaNoEncontradaException.class, () -> pagoService.procesarPago(request));
        verify(gestoPagoSendTxClient, never()).sendTx(anyString(), anyString());
    }

    @Test
    @DisplayName("Cliente inactivo lanza ValidacionNegocioException")
    void clienteInactivoLanzaExcepcion() {
        String numCuenta = "1234567890123456";
        Cuenta cuenta = crearCuentaValida(numCuenta, new BigDecimal("100.00"));
        cuenta.getCliente().setActivo(false);

        when(cuentaRepository.findByNumeroCuenta(numCuenta)).thenReturn(Optional.of(cuenta));

        PagoRequest request = PagoRequest.builder()
                .numCuenta(numCuenta)
                .idProd(582)
                .idSer(133)
                .monto(new BigDecimal("10.00"))
                .twoFactorCode("123456")
                .build();

        ValidacionNegocioException ex = assertThrows(ValidacionNegocioException.class,
                () -> pagoService.procesarPago(request));
        assertTrue(ex.getMessage().contains("no se encuentra activo"));
        verify(gestoPagoSendTxClient, never()).sendTx(anyString(), anyString());
    }

    @Test
    @DisplayName("Saldo insuficiente lanza ValidacionNegocioException sin llamar al proveedor")
    void saldoInsuficienteLanzaExcepcion() {
        String numCuenta = "1234567890123456";
        Cuenta cuenta = crearCuentaValida(numCuenta, new BigDecimal("5.00"));

        when(cuentaRepository.findByNumeroCuenta(numCuenta)).thenReturn(Optional.of(cuenta));

        // Para permitir monto 10.00 con saldo 5.00
        PagoRequest request = PagoRequest.builder()
                .numCuenta(numCuenta)
                .idProd(582)
                .idSer(133)
                .monto(new BigDecimal("10.00"))
                .twoFactorCode("123456")
                .build();

        ValidacionNegocioException ex = assertThrows(ValidacionNegocioException.class,
                () -> pagoService.procesarPago(request));
        assertTrue(ex.getMessage().contains("Saldo insuficiente"));
        verify(gestoPagoSendTxClient, never()).sendTx(anyString(), anyString());
    }

    @Test
    @DisplayName("2FA inválido lanza ValidacionNegocioException")
    void dosFactoresInvalidoLanzaExcepcion() {
        String numCuenta = "1234567890123456";
        Cuenta cuenta = crearCuentaValida(numCuenta, new BigDecimal("100.00"));

        when(cuentaRepository.findByNumeroCuenta(numCuenta)).thenReturn(Optional.of(cuenta));
        doThrow(new ValidacionNegocioException("Código 2FA incorrecto"))
                .when(twoFactorService).validarCodigo(eq(numCuenta), eq("000000"));

        PagoRequest request = PagoRequest.builder()
                .numCuenta(numCuenta)
                .idProd(582)
                .idSer(133)
                .monto(new BigDecimal("10.00"))
                .twoFactorCode("000000")
                .build();

        ValidacionNegocioException ex = assertThrows(ValidacionNegocioException.class,
                () -> pagoService.procesarPago(request));
        assertTrue(ex.getMessage().contains("2FA"));
        verify(gestoPagoSendTxClient, never()).sendTx(anyString(), anyString());
    }

    @Test
    @DisplayName("Conciliación exitosa con confirmTx (Código 06) debita saldo y confirma transacción")
    void conciliarTransaccionExitosa() {
        String upc = "7d3f6cb2b5";
        String numCuenta = "1234567890123456";
        Cuenta cuenta = crearCuentaValida(numCuenta, new BigDecimal("100.00"));
        when(cuentaRepository.findByNumeroCuenta(numCuenta)).thenReturn(Optional.of(cuenta));

        TransaccionPago tx = new TransaccionPago();
        tx.setUpc(upc);
        tx.setNumeroCuenta(numCuenta);
        tx.setMonto(new BigDecimal("10.00"));
        tx.setPayloadCifrado("payloadDummy");
        tx.setEstatus("PENDIENTE_CONCILIACION");
        tx.setFechaCreacion(LocalDateTime.now());

        when(transaccionPagoRepository.findByUpc(upc)).thenReturn(Optional.of(tx));

        String xmlConfirm = "<?xml version='1.0' encoding='UTF-8'?>"
                + "<RESPONSE>"
                + "    <NUM_AUTORIZACION>100024988</NUM_AUTORIZACION>"
                + "    <MENSAJE>"
                + "        <CODIGO>06</CODIGO>"
                + "        <TEXTO>La transaccion fue aplicada exitosamente. Autorizacion [100024988] idTicket[291437188]</TEXTO>"
                + "        <ID_TX>291437188</ID_TX>"
                + "    </MENSAJE>"
                + "</RESPONSE>";

        when(gestoPagoSendTxClient.confirmTx(anyString(), anyString())).thenReturn(xmlConfirm);

        PagoResponse respuesta = pagoService.conciliarTransaccion(upc);

        assertNotNull(respuesta);
        assertEquals("06", respuesta.getCodigoRespuesta());
        assertEquals("291437188", respuesta.getIdTx());
        assertEquals("100024988", respuesta.getNumAutorizacion());
        assertEquals(new BigDecimal("90.00"), respuesta.getNuevoSaldo());

        verify(saldoRepository, times(1)).save(any(Saldo.class));
    }
}
