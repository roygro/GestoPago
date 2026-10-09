package com.proyecto.servicios.entity.sf;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "cuentas")
@Getter
@Setter
public class Cuenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(name = "numero_cuenta", nullable = false, length = 16)
    private String numeroCuenta;

    @Column(name = "fecha_apertura", nullable = false)
    private LocalDateTime fechaApertura;

    @Column(name = "estatus", nullable = false, length = 10)
    private String estatus = "ACTIVA";

    @OneToOne(mappedBy = "cuenta", cascade = CascadeType.ALL, orphanRemoval = true)
    private Saldo saldo;

    @PrePersist
    protected void alCrear() {
        if (this.fechaApertura == null) this.fechaApertura = LocalDateTime.now();
        if (this.estatus == null) this.estatus = "ACTIVA";
    }
}