package com.proyecto.servicios.services;

import com.proyecto.servicios.client.ProductoClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.exception.ProductoAuthenticationException;
import com.proyecto.servicios.exception.ProductoEmptyResponseException;
import com.proyecto.servicios.exception.ProductoIntegrationException;
import com.proyecto.servicios.exception.ProductoTimeoutException;
import com.proyecto.servicios.model.producto.ProductoDTO;
import com.proyecto.servicios.model.producto.ProductoListResponse;
import com.proyecto.servicios.model.producto.ProductoServiceResponse;
import com.proyecto.servicios.service.GestoPagoTokenService;
import com.proyecto.servicios.service.Impl.ProductoServiceImpl;
import feign.FeignException;
import feign.RetryableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductoServiceImplTest {

    @Mock
    private ProductoClient productoClient;

    @Mock
    private GestoPagoTokenService gestoPagoTokenService;

    private ProductoServiceImpl productoService;

    @BeforeEach
    void setUp() {
        productoService = new ProductoServiceImpl(productoClient, gestoPagoTokenService);
        ReflectionTestUtils.setField(productoService, "idDistribuidor", 83);
        ReflectionTestUtils.setField(productoService, "codigoDispositivo", "GPS83-TPV-17");

        // Por defecto asumimos que SI hay token vigente.
        // Los tests que necesiten lo contrario lo sobrescriben explicitamente.
        GestoPagoToken tokenActivo = new GestoPagoToken();
        tokenActivo.setToken("token-de-prueba");
        lenient().when(gestoPagoTokenService.obtenerTokenActivo(any(), anyString()))
                .thenReturn(Optional.of(tokenActivo));
    }

    /**
     * Construye un producto con la misma forma que devuelve GestoPago
     * (atributos del elemento <producto> en el XML de getProductList.do).
     */
    private ProductoDTO productoDePrueba() {
        ProductoDTO producto = new ProductoDTO();
        producto.setServicio("TELCEL");
        producto.setNombreProducto("Telcel $50");
        producto.setIdServicio(1);
        producto.setIdProducto(101);
        producto.setIdCatTipoServicio(1);
        producto.setTipoFront("TAE");
        producto.setHasDigitoVerificador(false);
        producto.setPrecio(new BigDecimal("50.00"));
        producto.setShowAyuda(true);
        producto.setTipoReferencia("TELEFONO");
        producto.setLegend("Recarga Telcel");
        return producto;
    }

    @Test
    @DisplayName("Respuesta exitosa: devuelve codigo 0, mensaje de exito y la lista de productos")
    void obtenerListaProductos_respuestaExitosa_devuelveProductos() {
        ProductoDTO producto = productoDePrueba();

        ProductoServiceResponse externalResponse = new ProductoServiceResponse();
        externalResponse.setData(List.of(producto));

        when(productoClient.getProductList()).thenReturn(externalResponse);

        ProductoListResponse response = productoService.obtenerListaProductos();

        assertEquals(0, response.getCodigo().intValue());
        assertEquals("Éxito", response.getMensaje());
        assertEquals(1, response.getProductos().size());

        ProductoDTO devuelto = response.getProductos().get(0);
        assertEquals(101, devuelto.getIdProducto().intValue());
        assertEquals("Telcel $50", devuelto.getNombreProducto());
        assertEquals("TELCEL", devuelto.getServicio());
        assertEquals(new BigDecimal("50.00"), devuelto.getPrecio());
        assertEquals("TAE", devuelto.getTipoFront());
    }

    @Test
    @DisplayName("Lista vacia: no es un error, devuelve codigo 0 con lista vacia")
    void obtenerListaProductos_listaVacia_devuelveListaVacia() {
        ProductoServiceResponse externalResponse = new ProductoServiceResponse();
        externalResponse.setData(Collections.emptyList());

        when(productoClient.getProductList()).thenReturn(externalResponse);

        ProductoListResponse response = productoService.obtenerListaProductos();

        assertEquals(0, response.getCodigo().intValue());
        assertTrue(response.getProductos().isEmpty());
    }

    @Test
    @DisplayName("Sin token vigente: falla antes de llamar al cliente Feign (fail-fast)")
    void obtenerListaProductos_sinTokenVigente_lanzaAuthenticationException() {
        when(gestoPagoTokenService.obtenerTokenActivo(any(), anyString()))
                .thenReturn(Optional.empty());

        assertThrows(ProductoAuthenticationException.class,
                () -> productoService.obtenerListaProductos());

        verify(productoClient, never()).getProductList();
    }

    @Test
    @DisplayName("Respuesta nula del servicio externo")
    void obtenerListaProductos_respuestaNula_lanzaEmptyResponseException() {
        when(productoClient.getProductList()).thenReturn(null);

        assertThrows(ProductoEmptyResponseException.class,
                () -> productoService.obtenerListaProductos());
    }

    @Test
    @DisplayName("Respuesta no nula pero sin lista de productos")
    void obtenerListaProductos_dataNula_lanzaEmptyResponseException() {
        ProductoServiceResponse externalResponse = new ProductoServiceResponse();
        externalResponse.setData(null);
        when(productoClient.getProductList()).thenReturn(externalResponse);

        assertThrows(ProductoEmptyResponseException.class,
                () -> productoService.obtenerListaProductos());
    }

    @Test
    @DisplayName("401 desde el servicio externo")
    void obtenerListaProductos_errorAutenticacion_lanzaAuthenticationException() {
        FeignException.Unauthorized authError = mock(FeignException.Unauthorized.class);
        when(productoClient.getProductList()).thenThrow(authError);

        assertThrows(ProductoAuthenticationException.class,
                () -> productoService.obtenerListaProductos());
    }

    @Test
    @DisplayName("403 desde el servicio externo")
    void obtenerListaProductos_accesoProhibido_lanzaAuthenticationException() {
        FeignException.Forbidden forbiddenError = mock(FeignException.Forbidden.class);
        when(productoClient.getProductList()).thenThrow(forbiddenError);

        assertThrows(ProductoAuthenticationException.class,
                () -> productoService.obtenerListaProductos());
    }

    @Test
    @DisplayName("Timeout o error de comunicacion")
    void obtenerListaProductos_timeout_lanzaTimeoutException() {
        RetryableException timeoutError = mock(RetryableException.class);
        when(productoClient.getProductList()).thenThrow(timeoutError);

        assertThrows(ProductoTimeoutException.class,
                () -> productoService.obtenerListaProductos());
    }

    @Test
    @DisplayName("500 desde el servicio externo")
    void obtenerListaProductos_errorServidor_lanzaIntegrationException() {
        FeignException serverError = mock(FeignException.class);
        when(serverError.status()).thenReturn(500);
        when(productoClient.getProductList()).thenThrow(serverError);

        assertThrows(ProductoIntegrationException.class,
                () -> productoService.obtenerListaProductos());
    }

    @Test
    @DisplayName("Error inesperado no-Feign se envuelve en ProductoIntegrationException")
    void obtenerListaProductos_errorInesperado_lanzaIntegrationException() {
        when(productoClient.getProductList()).thenThrow(new IllegalStateException("boom"));

        assertThrows(ProductoIntegrationException.class,
                () -> productoService.obtenerListaProductos());
    }
}