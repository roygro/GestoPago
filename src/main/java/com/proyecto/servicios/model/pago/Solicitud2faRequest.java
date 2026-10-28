package com.proyecto.servicios.model.pago;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Petición para solicitar código de seguridad 2FA por correo")
public class Solicitud2faRequest {

    @NotBlank(message = "El número de cuenta es obligatorio")
    @JsonProperty("numCuenta")
    @JsonAlias({"numeroCuenta", "NumCuenta"})
    @Schema(description = "Número de cuenta de 16 dígitos", example = "7898384477103410")
    private String numCuenta;
}
