package com.proyecto.servicios.model.cliente;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LoginRequest {

    @NotBlank(message = "El usuario es obligatorio")
    private String usuario;

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;

    // Datos biométricos opcionales (reconocimiento facial)
    private Double distanciaInterocular;
    private Double anchoRostro;
    private java.math.BigDecimal confianzaDeteccion;
    private Integer numPuntosReferencia;
    private String plantillaFacial;
}