package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.cliente.CuentaResponse;
import com.proyecto.servicios.model.cliente.SaldoResponse;
import com.proyecto.servicios.service.CuentaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/cuentas")
public class CuentaController {

    @Autowired
    private CuentaService cuentaService;

    @GetMapping(value = "/{numeroCuenta}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CuentaResponse> consultarPorNumero(@PathVariable String numeroCuenta) {
        return ResponseEntity.ok(cuentaService.consultarPorNumeroCuenta(numeroCuenta));
    }

    @GetMapping(value = "/{numeroCuenta}/saldo", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SaldoResponse> consultarSaldo(@PathVariable String numeroCuenta) {
        return ResponseEntity.ok(cuentaService.consultarSaldo(numeroCuenta));
    }

    @GetMapping(value = "/activas", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CuentaResponse>> consultarActivas() {
        return ResponseEntity.ok(cuentaService.consultarActivas());
    }
}