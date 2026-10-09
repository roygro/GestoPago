package com.proyecto.servicios.model.cliente;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.proyecto.servicios.validation.EdadMinima;
import com.proyecto.servicios.validation.EstadoCivilValido;
import io.swagger.v3.oas.annotations.media.Schema;
import com.proyecto.servicios.validation.NacionalidadValida;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class ClienteRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚáéíóúÑñ ]{2,50}$", message = "El nombre solo debe contener letras y espacios (2-50 caracteres)")
    @Schema(example = "Juan")
    private String nombre;

    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚáéíóúÑñ ]{0,50}$", message = "El segundo nombre solo debe contener letras y espacios")
    @Schema(example = "Carlos")
    private String segundoNombre;

    @NotBlank(message = "El apellido paterno es obligatorio")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚáéíóúÑñ ]{2,50}$", message = "El apellido paterno solo debe contener letras y espacios (2-50 caracteres)")
    @Schema(example = "Pérez")
    private String apellidoPaterno;

    @NotBlank(message = "El apellido materno es obligatorio")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚáéíóúÑñ ]{2,50}$", message = "El apellido materno solo debe contener letras y espacios (2-50 caracteres)")
    @Schema(example = "López")
    private String apellidoMaterno;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @PastOrPresent(message = "La fecha de nacimiento no puede ser una fecha futura")
    @EdadMinima(value = 18)
    @Schema(description = "Formato yyyy-MM-dd. Debe ser mayor de 18 años", example = "1995-05-20")
    private LocalDate fechaNacimiento;

    @NotBlank(message = "La CURP es obligatoria")
    @Pattern(
            regexp = "^[A-Z]{4}\\d{6}[HM][A-Z]{5}[A-Z0-9]{2}$",
            message = "La CURP no tiene un formato válido (18 caracteres)"
    )
    @Schema(description = "18 caracteres, en mayúsculas", example = "PELJ950520HGTRPN09")
    private String curp;

    @NotBlank(message = "El RFC es obligatorio")
    @Pattern(
            regexp = "^[A-ZÑ&]{3,4}\\d{6}[A-Z0-9]{2,3}$",
            message = "El RFC no tiene un formato válido (12 o 13 caracteres)"
    )
    @Schema(description = "12 o 13 caracteres, en mayúsculas", example = "PELJ950520AB1")
    private String rfc;

    @NotBlank(message = "El sexo es obligatorio")
    @Pattern(regexp = "^(M|F|Otros)$", message = "El sexo debe ser 'M', 'F' u 'Otros'")
    @Schema(description = "M = masculino, F = femenino, Otros", allowableValues = {"M", "F", "Otros"}, example = "M")
    private String sexo;

    @NotBlank(message = "La nacionalidad es obligatoria")
    @NacionalidadValida
    @Schema(description = "Valor del catálogo, tal cual y con acentos (consultar GET /catalogos/nacionalidades)", example = "Mexicano")
    private String nacionalidad;

    @NotBlank(message = "El estado civil es obligatorio")
    @EstadoCivilValido
    @Schema(description = "Clave del catálogo (consultar GET /catalogos/estado-civil)", example = "SOLTERO")
    private String estadoCivil;

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
