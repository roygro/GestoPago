package com.proyecto.servicios.model.cliente;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class LoginRequest {

    @NotBlank(message = "El usuario es obligatorio")
    @Size(max = 50, message = "El usuario no debe exceder 50 caracteres")
    @Schema(example = "juanperez")
    private String usuario;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(max = 72, message = "La contraseña no debe exceder 72 caracteres")
    @Schema(example = "Password123")
    private String password;

    // Datos biométricos opcionales (reconocimiento facial)
    @Schema(description = "Opcional", example = "62.5")
    private Double distanciaInterocular;

    @Schema(description = "Opcional", example = "140.0")
    private Double anchoRostro;

    @DecimalMin(value = "0.00", message = "La confianza de detección debe estar entre 0 y 100")
    @DecimalMax(value = "100.00", message = "La confianza de detección debe estar entre 0 y 100")
    @Digits(integer = 3, fraction = 2, message = "La confianza de detección admite máximo 3 enteros y 2 decimales")
    @Schema(description = "Opcional, de 0 a 100", example = "95.50")
    private BigDecimal confianzaDeteccion;

    @Schema(description = "Opcional", example = "68")
    private Integer numPuntosReferencia;

    @Schema(description = "Opcional")
    private String plantillaFacial;
}
