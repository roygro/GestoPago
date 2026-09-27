package com.proyecto.servicios.model.cliente;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class LoginResponse {
    private String jwtToken;
    private LocalDateTime jwtFechaExpiracion;
    private Boolean sesionActiva;
    private Integer minutosExpiracionInactividad;
}