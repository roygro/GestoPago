package com.proyecto.servicios.model.pago;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

/**
 * Mapeo de la respuesta XML de confirmTx.do de GestoPago.
 * Código "06": Transacción aplicada exitosamente.
 * Código "70": Transacción no fue aplicada.
 */
@Data
@JacksonXmlRootElement(localName = "RESPONSE")
@JsonIgnoreProperties(ignoreUnknown = true)
public class GestoPagoConfirmTxResponse {

    @JacksonXmlProperty(localName = "NUM_AUTORIZACION")
    private String numAutorizacion;

    @JacksonXmlProperty(localName = "MENSAJE")
    private MensajeConfirmacion mensaje;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MensajeConfirmacion {
        @JacksonXmlProperty(localName = "CODIGO")
        private String codigo;

        @JacksonXmlProperty(localName = "TEXTO")
        private String texto;

        @JacksonXmlProperty(localName = "ID_TX")
        private String idTx;
    }

    public boolean fueAplicada() {
        return mensaje != null && "06".equals(mensaje.getCodigo());
    }

    public boolean noFueAplicada() {
        return mensaje != null && "70".equals(mensaje.getCodigo());
    }
}
