package com.proyecto.servicios.model.cliente;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Todos los filtros son opcionales y se combinan entre sí (AND)")
public class ClienteBusquedaRequest {

    @Positive(message = "El id debe ser mayor a cero")
    @Schema(example = "1")
    private Long id;

    @Size(max = 18, message = "La CURP no debe exceder 18 caracteres")
    @Pattern(regexp = "^[A-Za-z0-9]*$", message = "La CURP solo debe contener letras y números")
    @Schema(example = "PELJ950520HGTRPN09")
    private String curp;

    @Size(max = 13, message = "El RFC no debe exceder 13 caracteres")
    @Pattern(regexp = "^[A-Za-zÑñ&0-9]*$", message = "El RFC solo debe contener letras y números")
    @Schema(example = "PELJ950520AB1")
    private String rfc;

    @Email(message = "El correo electrónico no tiene un formato válido")
    @Size(max = 100, message = "El correo electrónico no debe exceder 100 caracteres")
    @Schema(example = "juan.perez@correo.com")
    private String correoElectronico;

    @Size(max = 50, message = "El nombre no debe exceder 50 caracteres")
    @Schema(description = "Coincidencia parcial, sin distinguir mayúsculas", example = "Juan")
    private String nombre;

    @Size(max = 50, message = "El apellido paterno no debe exceder 50 caracteres")
    @Schema(description = "Coincidencia parcial, sin distinguir mayúsculas", example = "Pérez")
    private String apellidoPaterno;

    @Pattern(regexp = "^\\d{16}$", message = "El número de cuenta debe contener exactamente 16 dígitos")
    @Schema(example = "7024827410255493")
    private String numeroCuenta;

    @Schema(description = "true = activos, false = dados de baja", example = "true")
    private Boolean activo;

    @Schema(description = "Registro desde (yyyy-MM-ddTHH:mm:ss)", example = "2026-01-01T00:00:00")
    private LocalDateTime fechaRegistroDesde;

    @Schema(description = "Registro hasta (yyyy-MM-ddTHH:mm:ss)", example = "2026-12-31T23:59:59")
    private LocalDateTime fechaRegistroHasta;
}