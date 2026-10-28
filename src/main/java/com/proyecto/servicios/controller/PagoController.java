package com.proyecto.servicios.controller;

import com.proyecto.servicios.exception.ErrorResponse;
import com.proyecto.servicios.model.RespuestaApi;
import com.proyecto.servicios.model.pago.PagoRequest;
import com.proyecto.servicios.model.pago.PagoResponse;
import com.proyecto.servicios.service.PagoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.proyecto.servicios.model.pago.Solicitud2faRequest;
import com.proyecto.servicios.model.pago.Solicitud2faResponse;
import com.proyecto.servicios.service.TwoFactorService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pagos")
@Tag(name = "Pagos", description = "Procesamiento y autorización de pagos de servicios y recargas (GestoPago / PuntoRed)")
public class PagoController {

    private final PagoService pagoService;
    private final TwoFactorService twoFactorService;

    public PagoController(PagoService pagoService, TwoFactorService twoFactorService) {
        this.pagoService = pagoService;
        this.twoFactorService = twoFactorService;
    }

    @Operation(summary = "Solicitar código 2FA por correo",
            description = "Genera un código aleatorio de 6 dígitos con vigencia de 5 minutos y lo envía al correo registrado del titular de la cuenta.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Código 2FA generado y enviado"),
            @ApiResponse(responseCode = "400", description = "La cuenta o cliente no se encuentra activo", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Cuenta no encontrada", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(value = "/solicitar-2fa", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RespuestaApi<Solicitud2faResponse>> solicitar2fa(@Valid @RequestBody Solicitud2faRequest request) {
        Solicitud2faResponse respuesta = twoFactorService.generarYEnviarCodigo(request.getNumCuenta());
        return ResponseEntity.ok(RespuestaApi.de(HttpStatus.OK,
                "Código 2FA enviado al correo " + respuesta.getCorreoDestino() + " (vigente por " + respuesta.getVigenciaMinutos() + " minutos)",
                respuesta));
    }

    @Operation(summary = "Procesar pago de servicio o recarga",
            description = "Valida cliente activo, saldo disponible y código 2FA; cifra el payload con AES-256 y envía la transacción a GestoPago. Si el proveedor responde '01', se debita el saldo y se confirma el pago.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pago procesado exitosamente (Código 01)"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o saldo insuficiente", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Cuenta o cliente no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "Transacción rechazada por el proveedor (Código distinto a 01)", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RespuestaApi<PagoResponse>> procesarPago(@Valid @RequestBody PagoRequest request) {
        PagoResponse respuesta = pagoService.procesarPago(request);
        return ResponseEntity.ok(RespuestaApi.de(HttpStatus.OK, "Pago realizado con éxito", respuesta));
    }

    @Operation(summary = "Conciliar transacción de pago pendiente (confirmTx.do)",
            description = "Consulta con el proveedor GestoPago el estado definitivo de una transacción que haya sufrido timeout o desconexión, aplicando el débito solo si fue exitosa.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transacción conciliada exitosamente"),
            @ApiResponse(responseCode = "400", description = "La transacción no existe o no requiere conciliación", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "El proveedor confirmó que la transacción no fue aplicada", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(value = "/conciliar/{upc}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RespuestaApi<PagoResponse>> conciliarTransaccion(@PathVariable("upc") String upc) {
        PagoResponse respuesta = pagoService.conciliarTransaccion(upc);
        return ResponseEntity.ok(RespuestaApi.de(HttpStatus.OK, "Conciliación procesada correctamente", respuesta));
    }
}
