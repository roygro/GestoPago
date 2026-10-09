package com.proyecto.servicios.model.cliente;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DomicilioDTO {

    @NotBlank(message = "La calle es obligatoria")
    @Size(min = 2, max = 100, message = "La calle debe tener entre 2 y 100 caracteres")
    @Schema(example = "Av. Hidalgo")
    private String calle;

    @NotBlank(message = "El número exterior es obligatorio")
    @Size(max = 10, message = "El número exterior no debe exceder 10 caracteres")
    @Schema(example = "123")
    private String numeroExterior;

    @Size(max = 10, message = "El número interior no debe exceder 10 caracteres")
    @Schema(example = "4B")
    private String numeroInterior;

    @NotBlank(message = "La colonia es obligatoria")
    @Size(max = 100, message = "La colonia no debe exceder 100 caracteres")
    @Schema(example = "Centro")
    private String colonia;

    @NotBlank(message = "El municipio es obligatorio")
    @Size(max = 100, message = "El municipio no debe exceder 100 caracteres")
    @Schema(example = "Dolores Hidalgo")
    private String municipio;

    @NotBlank(message = "El estado es obligatorio")
    @Size(max = 100, message = "El estado no debe exceder 100 caracteres")
    @Schema(example = "Guanajuato")
    private String estado;

    @NotBlank(message = "El código postal es obligatorio")
    @Pattern(regexp = "^\\d{5}$", message = "El código postal debe contener exactamente 5 dígitos")
    @Schema(example = "37800")
    private String codigoPostal;

    @NotBlank(message = "El país es obligatorio")
    @Size(max = 50, message = "El país no debe exceder 50 caracteres")
    @Schema(example = "México")
    private String pais;
}
