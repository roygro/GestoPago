package com.proyecto.servicios.model.pago;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

/**
 * Mapeo de la respuesta XML de GestoPago al método sendTx.do.
 * Un CODIGO igual a "01" y un ID_TX diferente de "-1" representan un pago exitoso.
 */
@Data
@JacksonXmlRootElement(localName = "RESPONSE")
@JsonIgnoreProperties(ignoreUnknown = true)
public class GestoPagoSendTxResponse {

    @JacksonXmlProperty(localName = "ID_TX")
    private String idTx;

    @JacksonXmlProperty(localName = "NUM_AUTORIZACION")
    private String numAutorizacion;

    @JacksonXmlProperty(localName = "SALDO")
    private String saldo;

    @JacksonXmlProperty(localName = "SALDO_F")
    private String saldoF;

    @JacksonXmlProperty(localName = "COMISION")
    private String comision;

    @JacksonXmlProperty(localName = "COMISION_F")
    private String comisionF;

    @JacksonXmlProperty(localName = "FECHA")
    private String fecha;

    @JacksonXmlProperty(localName = "MONTO")
    private String monto;

    @JacksonXmlProperty(localName = "MENSAJE")
    private MensajeTx mensaje;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MensajeTx {
        @JacksonXmlProperty(localName = "CODIGO")
        private String codigo;

        @JacksonXmlProperty(localName = "TEXTO")
        private String texto;

        @JacksonXmlProperty(localName = "REFERENCIA")
        private String referencia;
    }

    /**
     * Determina si la transacción fue exitosa según la documentación de PuntoRed / GestoPago:
     * CODIGO == "01" e ID_TX != "-1".
     */
    public boolean esExitoso() {
        return mensaje != null && "01".equals(mensaje.getCodigo()) && !"-1".equals(idTx);
    }
}
