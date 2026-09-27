package com.proyecto.servicios.model.cliente;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class ClienteActualizaRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚáéíóúÑñ ]{2,50}$")
    private String nombre;

    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚáéíóúÑñ ]{0,50}$")
    private String segundoNombre;

    @NotBlank(message = "El apellido paterno es obligatorio")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚáéíóúÑñ ]{2,50}$")
    private String apellidoPaterno;

    @NotBlank(message = "El apellido materno es obligatorio")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚáéíóúÑñ ]{2,50}$")
    private String apellidoMaterno;

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email
    @Size(max = 100)
    private String correoElectronico;

    @NotBlank(message = "El teléfono móvil es obligatorio")
    @Pattern(regexp = "^\\d{10}$", message = "El teléfono móvil debe contener exactamente 10 dígitos")
    private String telefonoMovil;

    @Pattern(regexp = "^\\d{10}$")
    private String telefonoAlternativo;

    @NotBlank(message = "La ocupación es obligatoria")
    private String ocupacion;

    private String empresa;

    @NotNull(message = "El ingreso mensual es obligatorio")
    @DecimalMin(value = "0.01", message = "El ingreso mensual debe ser mayor a cero")
    private BigDecimal ingresoMensual;

    @NotNull(message = "El domicilio es obligatorio")
    @Valid
    private DomicilioDTO domicilio;
}