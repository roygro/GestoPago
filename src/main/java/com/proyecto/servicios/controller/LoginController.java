package com.proyecto.servicios.controller;

import com.proyecto.servicios.exception.ErrorResponse;
import com.proyecto.servicios.model.RespuestaApi;
import com.proyecto.servicios.model.cliente.CredencialesRequest;
import com.proyecto.servicios.model.cliente.LoginRequest;
import com.proyecto.servicios.model.cliente.LoginResponse;
import com.proyecto.servicios.service.LoginService;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class LoginController {

    @Autowired
    private LoginService loginService;

    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Not Found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Conflict", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(value = "/credenciales", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RespuestaApi<LoginResponse>> crearCredenciales(@Valid @RequestBody CredencialesRequest req) {
        LoginResponse sesion = loginService.crearCredenciales(req.getClienteId(), req.getUsuario(), req.getPassword());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RespuestaApi.de(HttpStatus.CREATED, "Credenciales creadas correctamente", sesion));
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RespuestaApi<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse sesion = loginService.autenticar(request);
        return ResponseEntity.ok(RespuestaApi.de(HttpStatus.OK, "Inicio de sesión exitoso", sesion));
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(value = "/logout", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RespuestaApi<Void>> logout(HttpServletRequest request) {
        // El filtro ya validó que el header exista y empiece con "Bearer "
        String token = request.getHeader("Authorization").substring(7).trim();
        loginService.cerrarSesion(token);
        return ResponseEntity.ok(RespuestaApi.de(HttpStatus.OK, "Sesión cerrada correctamente", null));
    }
}