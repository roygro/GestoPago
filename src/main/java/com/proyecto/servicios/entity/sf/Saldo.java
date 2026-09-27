package com.proyecto.servicios.entity.sf;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "saldos")
@Getter
@Setter
public class Saldo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "cuenta_id", nullable = false, unique = true)
    private Cuenta cuenta;

    @Column(name = "saldo_disponible", nullable = false, precision = 15, scale = 2)
    private BigDecimal saldoDisponible = BigDecimal.ZERO;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @PrePersist
    @PreUpdate
    protected void alGuardar() {
        this.fechaActualizacion = LocalDateTime.now();
    }
}