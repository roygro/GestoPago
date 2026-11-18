package com.proyecto.servicios.model.pago;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistorialPagoDTO {
    private Long id;
    private String numeroCuenta;
    private Integer idServicio;
    private Integer idProducto;
    private BigDecimal monto;
    private String telefono;
    private String referencia;
    private String upc;
    private String idTxProveedor;
    private String numAutorizacion;
    private String codigoRespuesta;
    private String mensajeRespuesta;
    private String estatus;
    private LocalDateTime fechaCreacion;
}
