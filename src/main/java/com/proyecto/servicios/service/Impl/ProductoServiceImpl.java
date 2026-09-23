package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.ProductoClient;
import com.proyecto.servicios.exception.ProductoAuthenticationException;
import com.proyecto.servicios.exception.ProductoEmptyResponseException;
import com.proyecto.servicios.exception.ProductoIntegrationException;
import com.proyecto.servicios.exception.ProductoTimeoutException;
import com.proyecto.servicios.model.producto.ProductoDTO;
import com.proyecto.servicios.model.producto.ProductoListResponse;
import com.proyecto.servicios.model.producto.ProductoServiceResponse;
import com.proyecto.servicios.service.GestoPagoTokenService;
import com.proyecto.servicios.service.ProductoService;
import feign.FeignException;
import feign.RetryableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProductoServiceImpl implements ProductoService {

    private static final String LOG_INICIO = "Inicio invocación servicio de productos - getProductList";
    private static final String LOG_FIN = "Fin invocación servicio de productos - getProductList";

    private final ProductoClient productoClient;
    private final GestoPagoTokenService gestoPagoTokenService;
    private final ProductoCacheService productoCacheService;

    @Value("${gestopago.auth.id-distribuidor}")
    private Integer idDistribuidor;

    @Value("${gestopago.auth.codigo-dispositivo}")
    private String codigoDispositivo;

    public ProductoServiceImpl(ProductoClient productoClient,
                               GestoPagoTokenService gestoPagoTokenService,
                               ProductoCacheService productoCacheService) {
        this.productoClient = productoClient;
        this.gestoPagoTokenService = gestoPagoTokenService;
        this.productoCacheService = productoCacheService;
    }

    @Override
    public ProductoListResponse obtenerListaProductos() {
        log.info(LOG_INICIO);
        try {
            Optional<Map<String, List<ProductoDTO>>> cacheOpt = productoCacheService.obtenerDesdeCache();
            if (cacheOpt.isPresent()) {
                log.info("Productos obtenidos exitosamente desde la capa de caché");
                return construirRespuesta(cacheOpt.get(), "Éxito (Caché)");
            }

            Map<String, List<ProductoDTO>> productosAgrupados = consultarYActualizarCache();
            return construirRespuesta(productosAgrupados, "Éxito");

        } catch (FeignException.Unauthorized | FeignException.Forbidden ex) {
            log.error("Error de autenticación al consumir el servicio de productos. status={}", ex.status());
            throw new ProductoAuthenticationException(
                    "No fue posible autenticar la petición con el servicio de productos", ex);

        } catch (RetryableException ex) {
            log.error("Timeout o error de comunicación al consumir el servicio de productos: {}", ex.getMessage());
            throw new ProductoTimeoutException(
                    "Tiempo de espera agotado o error de conexión con el servicio de productos", ex);

        } catch (FeignException ex) {
            log.error("El servicio de productos respondió con un error. status={}", ex.status());
            throw new ProductoIntegrationException(
                    "El servicio de productos respondió con un error. status=" + ex.status(), ex);

        } catch (ProductoIntegrationException ex) {
            throw ex;

        } catch (Exception ex) {
            log.error("Error inesperado al consumir el servicio de productos: {}", ex.getMessage(), ex);
            throw new ProductoIntegrationException("Error inesperado al consumir el servicio de productos", ex);

        } finally {
            log.info(LOG_FIN);
        }
    }

    /**
     * Refresco programado del caché de productos, todos los días a las 6:00 am.
     * Fuerza una llamada real a GestoPago (sin leer caché) y actualiza Redis/BD,
     * para que a las 6:01 am el primer usuario del día ya encuentre datos frescos
     * sin tener que esperar la llamada externa.
     */
    @Scheduled(cron = "0 0 6 * * *")
    public void refrescarCacheProductos() {
        log.info("Iniciando refresco programado del caché de productos (6:00 am)");
        try {
            consultarYActualizarCache();
            log.info("Caché de productos refrescado correctamente");
        } catch (Exception ex) {
            log.error("Error al refrescar de forma programada el caché de productos: {}", ex.getMessage(), ex);
        }
    }

    /**
     * Verifica el token, llama a GestoPago, agrupa por tipoFront y guarda en caché.
     * Compartido entre la petición normal (cuando no hay caché vigente) y el cron de las 6am.
     */
    private Map<String, List<ProductoDTO>> consultarYActualizarCache() {
        if (gestoPagoTokenService.obtenerTokenActivo(idDistribuidor, codigoDispositivo).isEmpty()) {
            log.error("No hay un token vigente de GestoPago para consultar el servicio de productos");
            throw new ProductoAuthenticationException(
                    "No hay un token vigente para autenticar la petición con el servicio de productos");
        }

        ProductoServiceResponse externalResponse = productoClient.getProductList();

        if (externalResponse == null || externalResponse.getData() == null) {
            log.error("El servicio de productos respondió sin datos (respuesta nula o sin lista de productos)");
            throw new ProductoEmptyResponseException("El servicio de productos no devolvió información");
        }

        Map<String, List<ProductoDTO>> productosAgrupados = agruparPorTipoFront(externalResponse.getData());
        productoCacheService.guardarEnCache(productosAgrupados);
        return productosAgrupados;
    }

    private Map<String, List<ProductoDTO>> agruparPorTipoFront(List<ProductoDTO> productos) {
        return productos.stream()
                .collect(Collectors.groupingBy(
                        ProductoDTO::getTipoFront,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    private ProductoListResponse construirRespuesta(Map<String, List<ProductoDTO>> productosAgrupados, String mensaje) {
        ProductoListResponse response = new ProductoListResponse();
        response.setCodigo(0);
        response.setMensaje(mensaje);
        response.setProductos(productosAgrupados);
        return response;
    }
}