package com.proyecto.servicios.model.cliente;

import io.swagger.v3.oas.annotations.media.Schema;
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

    @NotNull(message = "El id del cliente es obligatorio")
    @Positive(message = "El id del cliente debe ser mayor a cero")
    @Schema(example = "1")
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Pattern(regexp = "^\\p{L}[\\p{L}' -]{1,49}$",
            message = "El nombre solo debe contener letras, espacios, apóstrofes o guiones (2-50 caracteres)")
    @Schema(example = "Juan")
    private String nombre;

    @Pattern(regexp = "^(\\p{L}[\\p{L}' -]{0,49})?$",
            message = "El segundo nombre solo debe contener letras, espacios, apóstrofes o guiones (máximo 50 caracteres)")
    @Schema(example = "Carlos")
    private String segundoNombre;

    @NotBlank(message = "El apellido paterno es obligatorio")
    @Pattern(regexp = "^\\p{L}[\\p{L}' -]{1,49}$",
            message = "El apellido paterno solo debe contener letras, espacios, apóstrofes o guiones (2-50 caracteres)")
    @Schema(example = "Pérez")
    private String apellidoPaterno;

    @NotBlank(message = "El apellido materno es obligatorio")
    @Pattern(regexp = "^\\p{L}[\\p{L}' -]{1,49}$",
            message = "El apellido materno solo debe contener letras, espacios, apóstrofes o guiones (2-50 caracteres)")
    @Schema(example = "López")
    private String apellidoMaterno;

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El correo electrónico no tiene un formato válido")
    @Size(max = 100, message = "El correo electrónico no debe exceder 100 caracteres")
    @Schema(example = "juan.perez@correo.com")
    private String correoElectronico;

    @NotBlank(message = "El teléfono móvil es obligatorio")
    @Pattern(regexp = "^\\d{10}$", message = "El teléfono móvil debe contener exactamente 10 dígitos")
    @Schema(example = "4181234567")
    private String telefonoMovil;

    @Pattern(regexp = "^\\d{10}$", message = "El teléfono alternativo debe contener exactamente 10 dígitos")
    @Schema(example = "4181234568")
    private String telefonoAlternativo;

    @NotBlank(message = "La ocupación es obligatoria")
    @Size(max = 100, message = "La ocupación no debe exceder 100 caracteres")
    @Schema(example = "Ingeniero")
    private String ocupacion;

    @Size(max = 150, message = "La empresa no debe exceder 150 caracteres")
    @Schema(example = "Empresa Demo SA de CV")
    private String empresa;

    @NotNull(message = "El ingreso mensual es obligatorio")
    @DecimalMin(value = "0.01", message = "El ingreso mensual debe ser mayor a cero")
    @Digits(integer = 10, fraction = 2, message = "El ingreso mensual debe tener máximo 10 dígitos enteros y 2 decimales")
    @Schema(description = "Monto con 2 decimales", example = "2000.00")
    private BigDecimal ingresoMensual;

    @NotNull(message = "El domicilio es obligatorio")
    @Valid
    private DomicilioDTO domicilio;


}
