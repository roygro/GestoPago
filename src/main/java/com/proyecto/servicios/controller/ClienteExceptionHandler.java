package com.proyecto.servicios.controller;

import com.proyecto.servicios.exception.cliente.*;
import com.proyecto.servicios.model.GenericResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.proyecto.servicios.controller")
public class ClienteExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GenericResponse> manejaValidacion(MethodArgumentNotValidException ex) {
        String detalle = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return construyeRespuesta(HttpStatus.BAD_REQUEST, "Error de validación - " + detalle);
    }

    @ExceptionHandler({CurpDuplicadaException.class, RfcDuplicadoException.class,
            CorreoDuplicadoException.class, ClienteYaRegistradoException.class})
    public ResponseEntity<GenericResponse> manejaConflicto(ClienteBusinessException ex) {
        return construyeRespuesta(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler({ClienteNoEncontradoException.class, CuentaNoEncontradaException.class})
    public ResponseEntity<GenericResponse> manejaNoEncontrado(ClienteBusinessException ex) {
        return construyeRespuesta(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ValidacionNegocioException.class)
    public ResponseEntity<GenericResponse> manejaValidacionNegocio(ValidacionNegocioException ex) {
        return construyeRespuesta(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    private ResponseEntity<GenericResponse> construyeRespuesta(HttpStatus status, String mensaje) {
        GenericResponse response = new GenericResponse();
        response.setCodigo(status.value());
        response.setMensaje(mensaje);
        return new ResponseEntity<>(response, status);
    }
}