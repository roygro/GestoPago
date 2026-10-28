package com.proyecto.servicios.entity.pago;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transacciones_pagos")
@Getter
@Setter
public class TransaccionPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_cuenta", nullable = false, length = 16)
    private String numeroCuenta;

    @Column(name = "id_servicio", nullable = false)
    private Integer idServicio;

    @Column(name = "id_producto", nullable = false)
    private Integer idProducto;

    @Column(name = "monto", nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "referencia", length = 100)
    private String referencia;

    @Column(name = "upc", nullable = false, unique = true, length = 15)
    private String upc;

    @Column(name = "id_tx_proveedor", length = 60)
    private String idTxProveedor;

    @Column(name = "num_autorizacion", length = 60)
    private String numAutorizacion;

    @Column(name = "codigo_respuesta", length = 10)
    private String codigoRespuesta;

    @Column(name = "mensaje_respuesta", columnDefinition = "text")
    private String mensajeRespuesta;

    @Column(name = "payload_cifrado", columnDefinition = "text")
    private String payloadCifrado;

    @Column(name = "estatus", nullable = false, length = 30)
    private String estatus = "INICIADA";

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @PrePersist
    protected void alCrear() {
        LocalDateTime ahora = LocalDateTime.now();
        if (this.fechaCreacion == null) this.fechaCreacion = ahora;
        if (this.fechaActualizacion == null) this.fechaActualizacion = ahora;
        if (this.estatus == null) this.estatus = "INICIADA";
    }

    @PreUpdate
    protected void alActualizar() {
        this.fechaActualizacion = LocalDateTime.now();
    }
}
