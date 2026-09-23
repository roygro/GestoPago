package com.proyecto.servicios.service.Impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.entity.producto.ProductoCache;
import com.proyecto.servicios.model.producto.ProductoDTO;
import com.proyecto.servicios.repositorys.producto.ProductoCacheRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class ProductoCacheService {

    private static final String CACHE_KEY = "productos:lista";
    private static final Duration TTL = Duration.ofHours(1);

    private final RedisTemplate<String, String> redisTemplate;
    private final ProductoCacheRepository productoCacheRepository;
    private final ObjectMapper objectMapper;

    public ProductoCacheService(RedisTemplate<String, String> redisTemplate,
                                ProductoCacheRepository productoCacheRepository,
                                ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.productoCacheRepository = productoCacheRepository;
        this.objectMapper = objectMapper;
    }

    public Optional<Map<String, List<ProductoDTO>>> obtenerDesdeCache() {
        try {
            String json = redisTemplate.opsForValue().get(CACHE_KEY);
            if (json != null) {
                log.info("Productos obtenidos desde Redis");
                return Optional.of(deserializar(json));
            }
        } catch (Exception ex) {
            log.warn("Redis no disponible al leer caché de productos: {}", ex.getMessage());
        }

        return productoCacheRepository.findByCacheKey(CACHE_KEY)
                .filter(cache -> cache.getFechaActualizacion().isAfter(LocalDateTime.now().minus(TTL)))
                .map(cache -> {
                    log.info("Productos obtenidos desde la base de datos (respaldo)");
                    return deserializar(cache.getValor());
                });
    }

    public void guardarEnCache(Map<String, List<ProductoDTO>> productosAgrupados) {
        String json = serializar(productosAgrupados);

        try {
            redisTemplate.opsForValue().set(CACHE_KEY, json, TTL);
            log.info("Productos guardados en Redis");
            return;
        } catch (Exception ex) {
            log.warn("Redis no disponible al guardar caché de productos, se usará la base de datos: {}", ex.getMessage());
        }

        ProductoCache cache = productoCacheRepository.findByCacheKey(CACHE_KEY)
                .orElseGet(ProductoCache::new);
        cache.setCacheKey(CACHE_KEY);
        cache.setValor(json);
        cache.setFechaActualizacion(LocalDateTime.now());
        productoCacheRepository.save(cache);
        log.info("Productos guardados en base de datos (respaldo, Redis no disponible)");
    }

    private String serializar(Map<String, List<ProductoDTO>> productosAgrupados) {
        try {
            return objectMapper.writeValueAsString(productosAgrupados);
        } catch (Exception ex) {
            throw new IllegalStateException("No fue posible serializar los productos agrupados", ex);
        }
    }

    private Map<String, List<ProductoDTO>> deserializar(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, List<ProductoDTO>>>() {});
        } catch (Exception ex) {
            throw new IllegalStateException("No fue posible deserializar los productos cacheados", ex);
        }
    }
}