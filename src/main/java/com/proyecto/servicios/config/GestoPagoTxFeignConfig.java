package com.proyecto.servicios.config;

import com.proyecto.servicios.service.GestoPagoTokenService;
import feign.Request;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

/**
 * Configuración aislada de Feign para el cliente de transacciones GestoPago (sendTx).
 * Inyecta dinámicamente el Bearer Token activo desde GestoPagoTokenService y define timeouts.
 */
public class GestoPagoTxFeignConfig {

    private final GestoPagoTokenService gestoPagoTokenService;

    @Value("${gestopago.auth.id-distribuidor}")
    private Integer idDistribuidor;

    @Value("${gestopago.auth.codigo-dispositivo}")
    private String codigoDispositivo;

    @Value("${gestopago.sendtx.connect-timeout-ms:5000}")
    private long connectTimeoutMs;

    @Value("${gestopago.sendtx.read-timeout-ms:15000}")
    private long readTimeoutMs;

    public GestoPagoTxFeignConfig(GestoPagoTokenService gestoPagoTokenService) {
        this.gestoPagoTokenService = gestoPagoTokenService;
    }

    @Bean
    public RequestInterceptor gestoPagoTxAuthRequestInterceptor() {
        return requestTemplate -> gestoPagoTokenService
                .obtenerTokenActivo(idDistribuidor, codigoDispositivo)
                .ifPresent(token -> requestTemplate.header("Authorization", "Bearer " + token.getToken()));
    }

    @Bean
    public Request.Options gestoPagoTxRequestOptions() {
        return new Request.Options(
                connectTimeoutMs, TimeUnit.MILLISECONDS,
                readTimeoutMs, TimeUnit.MILLISECONDS,
                true);
    }
}
