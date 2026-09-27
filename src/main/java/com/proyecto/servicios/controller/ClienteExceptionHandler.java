package com.proyecto.servicios.controller;

import com.proyecto.servicios.exception.cliente.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(basePackages = "com.proyecto.servicios.controller")
public class ClienteExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> manejaValidacion(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errores.put(error.getField(), error.getDefaultMessage());
        }
        return construyeRespuesta(HttpStatus.BAD_REQUEST, "Error de validación", errores);
    }

    @ExceptionHandler({CurpDuplicadaException.class, RfcDuplicadoException.class,
            CorreoDuplicadoException.class, ClienteYaRegistradoException.class})
    public ResponseEntity<Map<String, Object>> manejaConflicto(ClienteBusinessException ex) {
        return construyeRespuesta(HttpStatus.CONFLICT, ex.getMessage(), null);
    }

    @ExceptionHandler({ClienteNoEncontradoException.class, CuentaNoEncontradaException.class})
    public ResponseEntity<Map<String, Object>> manejaNoEncontrado(ClienteBusinessException ex) {
        return construyeRespuesta(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler(ValidacionNegocioException.class)
    public ResponseEntity<Map<String, Object>> manejaValidacionNegocio(ValidacionNegocioException ex) {
        return construyeRespuesta(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
    }

    private ResponseEntity<Map<String, Object>> construyeRespuesta(HttpStatus status, String mensaje, Object detalle) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("codigo", status.value());
        body.put("mensaje", mensaje);
        if (detalle != null) {
            body.put("errores", detalle);
        }
        return new ResponseEntity<>(body, status);
    }
}