package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.ProductoClient;
import com.proyecto.servicios.exception.ProductoAuthenticationException;
import com.proyecto.servicios.exception.ProductoEmptyResponseException;
import com.proyecto.servicios.exception.ProductoIntegrationException;
import com.proyecto.servicios.exception.ProductoTimeoutException;
import com.proyecto.servicios.model.producto.ProductoListResponse;
import com.proyecto.servicios.model.producto.ProductoServiceResponse;
import com.proyecto.servicios.service.GestoPagoTokenService;
import com.proyecto.servicios.service.ProductoService;
import feign.FeignException;
import feign.RetryableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ProductoServiceImpl implements ProductoService {

    private static final String LOG_INICIO = "Inicio invocación servicio externo de productos - getProductList";
    private static final String LOG_FIN = "Fin invocación servicio externo de productos - getProductList";

    private final ProductoClient productoClient;
    private final GestoPagoTokenService gestoPagoTokenService;

    @Value("${gestopago.auth.id-distribuidor}")
    private Integer idDistribuidor;

    @Value("${gestopago.auth.codigo-dispositivo}")
    private String codigoDispositivo;

    public ProductoServiceImpl(ProductoClient productoClient, GestoPagoTokenService gestoPagoTokenService) {
        this.productoClient = productoClient;
        this.gestoPagoTokenService = gestoPagoTokenService;
    }

    @Override
    public ProductoListResponse obtenerListaProductos() {
        log.info(LOG_INICIO);
        try {
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

            ProductoListResponse response = new ProductoListResponse();
            response.setCodigo(0);
            response.setMensaje("Éxito");
            response.setProductos(externalResponse.getData());
            return response;

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
}