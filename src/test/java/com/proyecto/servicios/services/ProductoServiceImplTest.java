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
import com.proyecto.servicios.service.Impl.ProductoCacheService;
import com.proyecto.servicios.service.Impl.ProductoServiceImpl;
import feign.FeignException;
import feign.RetryableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

    @Mock
    private ProductoCacheService productoCacheService;

    @Captor
    private ArgumentCaptor<Map<String, List<ProductoDTO>>> agrupadosCaptor;

    private ProductoServiceImpl productoService;

    @BeforeEach
    void setUp() {
        productoService = new ProductoServiceImpl(productoClient, gestoPagoTokenService, productoCacheService);
        ReflectionTestUtils.setField(productoService, "idDistribuidor", 83);
        ReflectionTestUtils.setField(productoService, "codigoDispositivo", "GPS83-TPV-17");

        // Por defecto asumimos que SI hay token vigente.
        // Los tests que necesiten lo contrario lo sobrescriben explicitamente.
        GestoPagoToken tokenActivo = new GestoPagoToken();
        tokenActivo.setToken("token-de-prueba");
        lenient().when(gestoPagoTokenService.obtenerTokenActivo(any(), anyString()))
                .thenReturn(Optional.of(tokenActivo));

        // Por defecto NO hay cache vigente: la peticion va al servicio externo.
        // Los tests de cache lo sobrescriben explicitamente.
        lenient().when(productoCacheService.obtenerDesdeCache()).thenReturn(Optional.empty());
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Construye un producto con la misma forma que devuelve GestoPago
     * (atributos del elemento <producto> en el XML de getProductList.do).
     */
    private ProductoDTO productoDePrueba() {
        return productoDePrueba(101, "Telcel $50", "TAE");
    }

    private ProductoDTO productoDePrueba(int idProducto, String nombreProducto, String tipoFront) {
        ProductoDTO producto = new ProductoDTO();
        producto.setServicio("TELCEL");
        producto.setNombreProducto(nombreProducto);
        producto.setIdServicio(1);
        producto.setIdProducto(idProducto);
        producto.setIdCatTipoServicio(1);
        producto.setTipoFront(tipoFront);
        producto.setHasDigitoVerificador(false);
        producto.setPrecio(new BigDecimal("50.00"));
        producto.setShowAyuda(true);
        producto.setTipoReferencia("TELEFONO");
        producto.setLegend("Recarga Telcel");
        return producto;
    }

    private ProductoServiceResponse respuestaExterna(ProductoDTO... productos) {
        ProductoServiceResponse externalResponse = new ProductoServiceResponse();
        externalResponse.setData(List.of(productos));
        return externalResponse;
    }

    // ------------------------------------------------------------------
    // Peticion normal (sin cache vigente)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Respuesta exitosa: devuelve codigo 0, mensaje de exito y los productos agrupados")
    void obtenerListaProductos_respuestaExitosa_devuelveProductos() {
        when(productoClient.getProductList()).thenReturn(respuestaExterna(productoDePrueba()));

        ProductoListResponse response = productoService.obtenerListaProductos();

        assertEquals(0, response.getCodigo().intValue());
        assertEquals("Éxito", response.getMensaje());
        assertEquals(1, response.getProductos().size());

        List<ProductoDTO> grupoTae = response.getProductos().get("TAE");
        assertEquals(1, grupoTae.size());

        ProductoDTO devuelto = grupoTae.get(0);
        assertEquals(101, devuelto.getIdProducto().intValue());
        assertEquals("Telcel $50", devuelto.getNombreProducto());
        assertEquals("TELCEL", devuelto.getServicio());
        assertEquals(new BigDecimal("50.00"), devuelto.getPrecio());
        assertEquals("TAE", devuelto.getTipoFront());
    }

    @Test
    @DisplayName("Agrupa los productos por tipoFront conservando el orden de aparicion")
    void obtenerListaProductos_agrupaPorTipoFront() {
        when(productoClient.getProductList()).thenReturn(respuestaExterna(
                productoDePrueba(1, "Telcel $50", "1"),
                productoDePrueba(2, "CFE", "2"),
                productoDePrueba(3, "Telcel $100", "1"),
                productoDePrueba(4, "Saldo CFE", "4")));

        ProductoListResponse response = productoService.obtenerListaProductos();

        Map<String, List<ProductoDTO>> productos = response.getProductos();
        assertEquals(List.of("1", "2", "4"), new ArrayList<>(productos.keySet()));
        assertEquals(2, productos.get("1").size());
        assertEquals(1, productos.get("2").size());
        assertEquals(1, productos.get("4").size());
        assertEquals("Telcel $50", productos.get("1").get(0).getNombreProducto());
        assertEquals("Telcel $100", productos.get("1").get(1).getNombreProducto());
    }

    @Test
    @DisplayName("Lista vacia: no es un error, devuelve codigo 0 sin grupos")
    void obtenerListaProductos_listaVacia_devuelveListaVacia() {
        ProductoServiceResponse externalResponse = new ProductoServiceResponse();
        externalResponse.setData(Collections.emptyList());

        when(productoClient.getProductList()).thenReturn(externalResponse);

        ProductoListResponse response = productoService.obtenerListaProductos();

        assertEquals(0, response.getCodigo().intValue());
        assertTrue(response.getProductos().isEmpty());
    }

    // ------------------------------------------------------------------
    // Cache
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Con cache vigente: responde desde cache y NO llama al servicio externo")
    void obtenerListaProductos_conCacheVigente_noLlamaAlServicioExterno() {
        Map<String, List<ProductoDTO>> cacheado =
                Map.of("1", List.of(productoDePrueba(1, "Telcel $50", "1")));
        when(productoCacheService.obtenerDesdeCache()).thenReturn(Optional.of(cacheado));

        ProductoListResponse response = productoService.obtenerListaProductos();

        assertEquals(0, response.getCodigo().intValue());
        assertEquals("Éxito (Caché)", response.getMensaje());
        assertEquals(cacheado, response.getProductos());

        verify(productoClient, never()).getProductList();
        verify(productoCacheService, never()).guardarEnCache(any());
    }

    @Test
    @DisplayName("Sin cache vigente: llama al servicio externo y guarda el resultado agrupado en cache")
    void obtenerListaProductos_sinCache_llamaAlServicioYGuardaEnCache() {
        when(productoClient.getProductList()).thenReturn(respuestaExterna(
                productoDePrueba(1, "Telcel $50", "1"),
                productoDePrueba(2, "CFE", "2")));

        productoService.obtenerListaProductos();

        verify(productoClient).getProductList();
        verify(productoCacheService).guardarEnCache(agrupadosCaptor.capture());

        Map<String, List<ProductoDTO>> guardado = agrupadosCaptor.getValue();
        assertEquals(List.of("1", "2"), new ArrayList<>(guardado.keySet()));
    }

    @Test
    @DisplayName("Si el servicio externo falla, NO se guarda nada en cache")
    void obtenerListaProductos_errorDelServicioExterno_noGuardaEnCache() {
        FeignException serverError = mock(FeignException.class);
        when(serverError.status()).thenReturn(500);
        when(productoClient.getProductList()).thenThrow(serverError);

        assertThrows(ProductoIntegrationException.class,
                () -> productoService.obtenerListaProductos());

        verify(productoCacheService, never()).guardarEnCache(any());
    }

    // ------------------------------------------------------------------
    // Cron de las 6:00 am
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Cron: esta programado todos los dias a las 6:00 am")
    void refrescarCacheProductos_estaProgramadoALas6am() throws NoSuchMethodException {
        Scheduled scheduled = ProductoServiceImpl.class
                .getMethod("refrescarCacheProductos")
                .getAnnotation(Scheduled.class);

        assertNotNull(scheduled, "El metodo debe tener @Scheduled");
        // Si mas adelante la expresion pasa a una property o se agrega zone,
        // actualiza esta asercion.
        assertEquals("0 0 6 * * *", scheduled.cron());
    }

    @Test
    @DisplayName("Cron: fuerza la consulta real (sin leer cache) y actualiza el cache")
    void refrescarCacheProductos_llamaAlServicioExternoYGuardaEnCache() {
        // Aunque exista un cache vigente, el cron debe ir siempre al servicio externo.
        lenient().when(productoCacheService.obtenerDesdeCache())
                .thenReturn(Optional.of(Map.of()));
        when(productoClient.getProductList()).thenReturn(respuestaExterna(productoDePrueba()));

        productoService.refrescarCacheProductos();

        verify(productoCacheService, never()).obtenerDesdeCache();
        verify(productoClient).getProductList();
        verify(productoCacheService).guardarEnCache(any());
    }

    @Test
    @DisplayName("Cron: si el servicio externo falla, no propaga la excepcion ni guarda cache")
    void refrescarCacheProductos_siFallaElServicio_noLanzaExcepcion() {
        when(productoClient.getProductList()).thenThrow(new IllegalStateException("boom"));

        assertDoesNotThrow(() -> productoService.refrescarCacheProductos());

        verify(productoCacheService, never()).guardarEnCache(any());
    }

    @Test
    @DisplayName("Cron: sin token vigente no llama al cliente ni propaga la excepcion")
    void refrescarCacheProductos_sinToken_noLlamaAlClienteNiLanzaExcepcion() {
        when(gestoPagoTokenService.obtenerTokenActivo(any(), anyString()))
                .thenReturn(Optional.empty());

        assertDoesNotThrow(() -> productoService.refrescarCacheProductos());

        verify(productoClient, never()).getProductList();
        verify(productoCacheService, never()).guardarEnCache(any());
    }

    // ------------------------------------------------------------------
    // Manejo de errores (sin cambios respecto a la version anterior)
    // ------------------------------------------------------------------

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