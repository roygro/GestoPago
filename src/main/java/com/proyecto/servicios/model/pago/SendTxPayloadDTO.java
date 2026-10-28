package com.proyecto.servicios.model.pago;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Carga útil (payload) interna enviada a sendTx.do de GestoPago.
 * Este objeto se serializa en JSON y se cifra con AES-256-CBC antes de enviarse en el parámetro 'signed'.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SendTxPayloadDTO {

    private Integer idProducto;

    private Integer idServicio;

    private String telefono;

    private String horaLocal;

    private String upc;

    private String unidad;

    private String referencia;

    private Double montoPago;
}
