package com.proyecto.servicios.entity.sf;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "login")
@Getter
@Setter
public class Login {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(name = "usuario", nullable = false, length = 50)
    private String usuario;

    @Column(name = "password_hash", nullable = false, columnDefinition = "text")
    private String passwordHash;

    @Column(name = "jwt_token", nullable = false, columnDefinition = "text")
    private String jwtToken;

    @Column(name = "jwt_fecha_expiracion", nullable = false)
    private LocalDateTime jwtFechaExpiracion;

    @Column(name = "sesion_activa", nullable = false)
    private Boolean sesionActiva = true;

    @Column(name = "fecha_inicio_sesion", nullable = false)
    private LocalDateTime fechaInicioSesion;

    @Column(name = "ultima_actividad", nullable = false)
    private LocalDateTime ultimaActividad;

    @Column(name = "minutos_expiracion_inactividad", nullable = false)
    private Integer minutosExpiracionInactividad = 5;

    @Column(name = "distancia_interocular")
    private Double distanciaInterocular;

    @Column(name = "ancho_rostro")
    private Double anchoRostro;

    @Column(name = "confianza_deteccion", precision = 5, scale = 2)
    private BigDecimal confianzaDeteccion;

    @Column(name = "num_puntos_referencia")
    private Integer numPuntosReferencia;

    @Column(name = "plantilla_facial", columnDefinition = "text")
    private String plantillaFacial;

    @PrePersist
    protected void alCrear() {
        LocalDateTime ahora = LocalDateTime.now();
        if (this.fechaInicioSesion == null) this.fechaInicioSesion = ahora;
        if (this.ultimaActividad == null) this.ultimaActividad = ahora;
        if (this.sesionActiva == null) this.sesionActiva = true;
        if (this.minutosExpiracionInactividad == null) this.minutosExpiracionInactividad = 5;
    }
}