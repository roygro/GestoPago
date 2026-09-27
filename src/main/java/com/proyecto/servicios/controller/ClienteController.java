package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.cliente.ClienteActualizaRequest;
import com.proyecto.servicios.model.cliente.ClienteRequest;
import com.proyecto.servicios.model.cliente.ClienteResponse;
import com.proyecto.servicios.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/clientes")
public class ClienteController {

    @Autowired
    private ClienteService clienteService;

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponse> registrar(@Valid @RequestBody ClienteRequest request) {
        ClienteResponse creado = clienteService.registrarCliente(request);
        return new ResponseEntity<>(creado, HttpStatus.CREATED);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ClienteResponse>> consultarTodos() {
        return ResponseEntity.ok(clienteService.consultarTodos());
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponse> consultarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.consultarPorId(id));
    }

    @GetMapping(value = "/curp/{curp}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponse> consultarPorCurp(@PathVariable String curp) {
        return ResponseEntity.ok(clienteService.consultarPorCurp(curp));
    }

    @GetMapping(value = "/rfc/{rfc}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponse> consultarPorRfc(@PathVariable String rfc) {
        return ResponseEntity.ok(clienteService.consultarPorRfc(rfc));
    }

    @GetMapping(value = "/correo/{correo}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponse> consultarPorCorreo(@PathVariable String correo) {
        return ResponseEntity.ok(clienteService.consultarPorCorreo(correo));
    }

    @GetMapping(value = "/activos", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ClienteResponse>> consultarActivos() {
        return ResponseEntity.ok(clienteService.consultarActivos());
    }

    @GetMapping(value = "/rango-fechas", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ClienteResponse>> consultarPorRangoFechas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        return ResponseEntity.ok(clienteService.consultarPorRangoFechas(desde, hasta));
    }

    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponse> actualizar(@PathVariable Long id,
                                                      @Valid @RequestBody ClienteActualizaRequest request) {
        return ResponseEntity.ok(clienteService.actualizarCliente(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> darDeBaja(@PathVariable Long id) {
        clienteService.darDeBajaLogica(id);
        return ResponseEntity.noContent().build();
    }
}