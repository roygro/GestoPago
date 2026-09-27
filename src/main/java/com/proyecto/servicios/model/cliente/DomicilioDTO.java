package com.proyecto.servicios.model.cliente;

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
    @Size(min = 2, max = 100)
    private String calle;

    @NotBlank(message = "El número exterior es obligatorio")
    private String numeroExterior;

    private String numeroInterior;

    @NotBlank(message = "La colonia es obligatoria")
    private String colonia;

    @NotBlank(message = "El municipio es obligatorio")
    private String municipio;

    @NotBlank(message = "El estado es obligatorio")
    private String estado;

    @NotBlank(message = "El código postal es obligatorio")
    @Pattern(regexp = "^\\d{5}$", message = "El código postal debe contener exactamente 5 dígitos")
    private String codigoPostal;

    @NotBlank(message = "El país es obligatorio")
    private String pais;
}