package com.proyecto.servicios.config;

import com.proyecto.servicios.service.GestoPagoTokenService;
import feign.Request;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

public class ProductoFeignConfig {

    private final GestoPagoTokenService gestoPagoTokenService;

    @Value("${gestopago.auth.id-distribuidor}")
    private Integer idDistribuidor;

    @Value("${gestopago.auth.codigo-dispositivo}")
    private String codigoDispositivo;

    @Value("${producto.service.connect-timeout-ms:5000}")
    private long connectTimeoutMs;

    @Value("${producto.service.read-timeout-ms:10000}")
    private long readTimeoutMs;

    public ProductoFeignConfig(GestoPagoTokenService gestoPagoTokenService) {
        this.gestoPagoTokenService = gestoPagoTokenService;
    }

    @Bean
    public RequestInterceptor productoAuthRequestInterceptor() {
        return requestTemplate -> gestoPagoTokenService
                .obtenerTokenActivo(idDistribuidor, codigoDispositivo)
                .ifPresent(token -> requestTemplate.header("Authorization", "Bearer " + token.getToken()));
    }

    @Bean
    public Request.Options productoRequestOptions() {
        return new Request.Options(
                connectTimeoutMs, TimeUnit.MILLISECONDS,
                readTimeoutMs, TimeUnit.MILLISECONDS,
                true);
    }
}