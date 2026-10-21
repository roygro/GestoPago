package com.proyecto.servicios.model.cliente;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CredencialesRequest {

    @NotNull(message = "El clienteId es obligatorio")
    @Positive(message = "El clienteId debe ser mayor a cero")
    @Schema(description = "ID del cliente al que se le crean las credenciales", example = "1")
    private Long clienteId;

    @NotBlank(message = "El usuario es obligatorio")
    @Pattern(regexp = "^[A-Za-z0-9._-]{4,50}$",
            message = "El usuario debe tener entre 4 y 50 caracteres: letras, números, punto, guion o guion bajo")
    @Schema(example = "juanperez")
    private String usuario;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)\\S+$",
            message = "La contraseña debe incluir mayúscula, minúscula y número, sin espacios")
    @Schema(example = "Password123")
    private String password;
}