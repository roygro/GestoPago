package com.proyecto.servicios.client;

import com.proyecto.servicios.config.GestoPagoTxFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Cliente Feign para el endpoint de envío de transacciones de GestoPago.
 * Realiza un POST con application/x-www-form-urlencoded enviando en el CUERPO:
 * - random: timestamp o cadena aleatoria
 * - signed: payload JSON cifrado con AES-256 en Base64 URL-encoded
 */
@FeignClient(
        name = "gestoPagoSendTxClient",
        url = "${gestopago.auth.url}",
        configuration = GestoPagoTxFeignConfig.class
)
public interface GestoPagoSendTxClient {

    @PostMapping(
            value = "/sistema/service/sendTx.do",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    String sendTx(@RequestBody String formBody);

    @PostMapping(
            value = "/sistema/service/confirmTx.do",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    String confirmTx(@RequestBody String formBody);
}
