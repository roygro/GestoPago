package com.proyecto.servicios.model.pago;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Respuesta de confirmación de envío de código 2FA")
public class Solicitud2faResponse {

    @Schema(description = "Número de cuenta consultado", example = "7898384477103410")
    private String numeroCuenta;

    @Schema(description = "Correo ofuscado al que se envió el código", example = "a*****z@empresademo.com")
    private String correoDestino;

    @Schema(description = "Vigencia en minutos del código de seguridad", example = "5")
    private Integer vigenciaMinutos;
}
