package com.proyecto.servicios.controller;

import com.proyecto.servicios.exception.ErrorResponse;
import com.proyecto.servicios.model.RespuestaApi;
import com.proyecto.servicios.model.cliente.CuentaConsultaRequest;
import com.proyecto.servicios.model.cliente.CuentaResponse;
import com.proyecto.servicios.model.cliente.SaldoResponse;
import com.proyecto.servicios.service.CuentaService;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cuentas")
public class CuentaController {

    @Autowired
    private CuentaService cuentaService;

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Not Found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(value = "/consultar", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RespuestaApi<CuentaResponse>> consultarPorNumero(@Valid @RequestBody CuentaConsultaRequest request) {
        CuentaResponse cuenta = cuentaService.consultarPorNumeroCuenta(request.getNumeroCuenta());
        return ResponseEntity.ok(RespuestaApi.de(HttpStatus.OK, "Cuenta consultada correctamente", cuenta));
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Not Found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(value = "/saldo", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RespuestaApi<SaldoResponse>> consultarSaldo(@Valid @RequestBody CuentaConsultaRequest request) {
        SaldoResponse saldo = cuentaService.consultarSaldo(request.getNumeroCuenta());
        return ResponseEntity.ok(RespuestaApi.de(HttpStatus.OK, "Saldo consultado correctamente", saldo));
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK")
    })
    @GetMapping(value = "/activas", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RespuestaApi<List<CuentaResponse>>> consultarActivas() {
        List<CuentaResponse> cuentas = cuentaService.consultarActivas();
        return ResponseEntity.ok(RespuestaApi.de(HttpStatus.OK, "Cuentas activas consultadas correctamente", cuentas));
    }
}