package com.proyecto.servicios.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.entity.producto.ProductoCache;
import com.proyecto.servicios.model.producto.ProductoDTO;
import com.proyecto.servicios.repositorys.producto.ProductoCacheRepository;
import com.proyecto.servicios.service.Impl.ProductoCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas del cache de productos:
 *  - Escritura: Redis si esta disponible; si no, respaldo en base de datos.
 *  - Lectura: Redis primero; si no hay dato o Redis falla, base de datos (solo si sigue vigente).
 */
@ExtendWith(MockitoExtension.class)
class ProductoCacheServiceTest {

    private static final String CACHE_KEY = "productos:lista";

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ProductoCacheRepository productoCacheRepository;

    @Captor
    private ArgumentCaptor<ProductoCache> cacheCaptor;

    // ObjectMapper real (no mock): asi probamos la serializacion de verdad.
    private final ObjectMapper objectMapper = new ObjectMapper();

    private ProductoCacheService cacheService;

    @BeforeEach
    void setUp() {
        cacheService = new ProductoCacheService(redisTemplate, productoCacheRepository, objectMapper);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Map<String, List<ProductoDTO>> productosAgrupados() {
        ProductoDTO producto = new ProductoDTO();
        producto.setServicio("Telcel");
        producto.setNombreProducto("Telcel $50");
        producto.setIdServicio(133);
        producto.setIdProducto(405);
        producto.setIdCatTipoServicio(2);
        producto.setTipoFront("1");
        producto.setHasDigitoVerificador(false);
        producto.setPrecio(new BigDecimal("50"));
        producto.setShowAyuda(true);
        producto.setTipoReferencia("a");
        producto.setLegend("Recarga Telcel");
        return Map.of("1", List.of(producto));
    }

    private ProductoCache registroEnBd(String valor, LocalDateTime fechaActualizacion) {
        ProductoCache registro = new ProductoCache();
        registro.setId(1L);
        registro.setCacheKey(CACHE_KEY);
        registro.setValor(valor);
        registro.setFechaActualizacion(fechaActualizacion);
        return registro;
    }

    // ------------------------------------------------------------------
    // guardarEnCache
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Guardar: con Redis disponible guarda en Redis (TTL 1 hora) y NO toca la base de datos")
    void guardarEnCache_redisDisponible_guardaEnRedis() {
        cacheService.guardarEnCache(productosAgrupados());

        verify(valueOperations).set(eq(CACHE_KEY), anyString(), eq(Duration.ofHours(1)));
        verify(productoCacheRepository, never()).save(any());
    }

    @Test
    @DisplayName("Guardar: con Redis caido guarda en la base de datos como respaldo")
    void guardarEnCache_redisNoDisponible_guardaEnBaseDeDatos() {
        doThrow(new RedisConnectionFailureException("Redis caido"))
                .when(valueOperations).set(eq(CACHE_KEY), anyString(), any(Duration.class));
        when(productoCacheRepository.findByCacheKey(CACHE_KEY)).thenReturn(Optional.empty());

        cacheService.guardarEnCache(productosAgrupados());

        verify(productoCacheRepository).save(cacheCaptor.capture());
        ProductoCache guardado = cacheCaptor.getValue();
        assertEquals(CACHE_KEY, guardado.getCacheKey());
        assertTrue(guardado.getValor().contains("Telcel $50"));
        assertNotNull(guardado.getFechaActualizacion());
    }

    @Test
    @DisplayName("Guardar: con Redis caido y registro previo en BD, actualiza ese registro (no crea otro)")
    void guardarEnCache_redisNoDisponible_actualizaRegistroExistente() {
        ProductoCache existente = registroEnBd("{}", LocalDateTime.now().minusDays(1));
        doThrow(new RedisConnectionFailureException("Redis caido"))
                .when(valueOperations).set(eq(CACHE_KEY), anyString(), any(Duration.class));
        when(productoCacheRepository.findByCacheKey(CACHE_KEY)).thenReturn(Optional.of(existente));

        cacheService.guardarEnCache(productosAgrupados());

        verify(productoCacheRepository, times(1)).save(cacheCaptor.capture());
        assertSame(existente, cacheCaptor.getValue());
        assertNotEquals("{}", existente.getValor());
        assertTrue(existente.getFechaActualizacion().isAfter(LocalDateTime.now().minusMinutes(1)));
    }

    // ------------------------------------------------------------------
    // obtenerDesdeCache
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Leer: si Redis tiene el dato lo devuelve y no consulta la base de datos")
    void obtenerDesdeCache_redisTieneDatos_devuelveDesdeRedis() throws Exception {
        String json = objectMapper.writeValueAsString(productosAgrupados());
        when(valueOperations.get(CACHE_KEY)).thenReturn(json);

        Optional<Map<String, List<ProductoDTO>>> resultado = cacheService.obtenerDesdeCache();

        assertTrue(resultado.isPresent());
        ProductoDTO producto = resultado.get().get("1").get(0);
        assertEquals("Telcel $50", producto.getNombreProducto());
        assertEquals(405, producto.getIdProducto().intValue());
        verify(productoCacheRepository, never()).findByCacheKey(anyString());
    }

    @Test
    @DisplayName("Leer: si Redis no tiene el dato usa la base de datos (registro vigente)")
    void obtenerDesdeCache_redisSinDatos_usaBaseDeDatosVigente() throws Exception {
        String json = objectMapper.writeValueAsString(productosAgrupados());
        when(valueOperations.get(CACHE_KEY)).thenReturn(null);
        when(productoCacheRepository.findByCacheKey(CACHE_KEY))
                .thenReturn(Optional.of(registroEnBd(json, LocalDateTime.now().minusMinutes(10))));

        Optional<Map<String, List<ProductoDTO>>> resultado = cacheService.obtenerDesdeCache();

        assertTrue(resultado.isPresent());
        assertEquals("Telcel $50", resultado.get().get("1").get(0).getNombreProducto());
    }

    @Test
    @DisplayName("Leer: si Redis esta caido usa la base de datos (registro vigente)")
    void obtenerDesdeCache_redisNoDisponible_usaBaseDeDatos() throws Exception {
        String json = objectMapper.writeValueAsString(productosAgrupados());
        when(valueOperations.get(CACHE_KEY))
                .thenThrow(new RedisConnectionFailureException("Redis caido"));
        when(productoCacheRepository.findByCacheKey(CACHE_KEY))
                .thenReturn(Optional.of(registroEnBd(json, LocalDateTime.now().minusMinutes(5))));

        Optional<Map<String, List<ProductoDTO>>> resultado = cacheService.obtenerDesdeCache();

        assertTrue(resultado.isPresent());
        assertEquals("Telcel $50", resultado.get().get("1").get(0).getNombreProducto());
    }

    @Test
    @DisplayName("Leer: un registro en BD con mas de 1 hora se considera vencido y se ignora")
    void obtenerDesdeCache_registroEnBdVencido_devuelveVacio() throws Exception {
        String json = objectMapper.writeValueAsString(productosAgrupados());
        when(valueOperations.get(CACHE_KEY)).thenReturn(null);
        when(productoCacheRepository.findByCacheKey(CACHE_KEY))
                .thenReturn(Optional.of(registroEnBd(json, LocalDateTime.now().minusHours(2))));

        Optional<Map<String, List<ProductoDTO>>> resultado = cacheService.obtenerDesdeCache();

        assertFalse(resultado.isPresent());
    }

    @Test
    @DisplayName("Leer: sin datos en Redis ni en BD devuelve vacio")
    void obtenerDesdeCache_sinDatosEnNingunLado_devuelveVacio() {
        when(valueOperations.get(CACHE_KEY)).thenReturn(null);
        when(productoCacheRepository.findByCacheKey(CACHE_KEY)).thenReturn(Optional.empty());

        Optional<Map<String, List<ProductoDTO>>> resultado = cacheService.obtenerDesdeCache();

        assertFalse(resultado.isPresent());
    }
}