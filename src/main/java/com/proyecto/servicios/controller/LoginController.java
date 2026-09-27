package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.cliente.LoginRequest;
import com.proyecto.servicios.model.cliente.LoginResponse;
import com.proyecto.servicios.service.LoginService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class LoginController {

    @Autowired
    private LoginService loginService;

    public static class CredencialesRequest {
        public Long clienteId;
        public String usuario;
        public String password;
    }

    @PostMapping(value = "/credenciales", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LoginResponse> crearCredenciales(@RequestBody CredencialesRequest req) {
        return ResponseEntity.ok(loginService.crearCredenciales(req.clienteId, req.usuario, req.password));
    }

    @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(loginService.autenticar(request));
    }

    @PostMapping(value = "/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String jwtToken) {
        loginService.cerrarSesion(jwtToken.replace("Bearer ", ""));
        return ResponseEntity.noContent().build();
    }
}