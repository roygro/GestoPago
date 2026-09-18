package com.proyecto.servicios.controller;

import com.proyecto.servicios.exception.ProductoAuthenticationException;
import com.proyecto.servicios.exception.ProductoEmptyResponseException;
import com.proyecto.servicios.exception.ProductoIntegrationException;
import com.proyecto.servicios.exception.ProductoTimeoutException;
import com.proyecto.servicios.model.GenericResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = ProductoController.class)
@Slf4j
public class ProductoExceptionHandler {

    @ExceptionHandler(ProductoAuthenticationException.class)
    public ResponseEntity<GenericResponse> handleAuthenticationError(ProductoAuthenticationException ex) {
        return build(HttpStatus.UNAUTHORIZED, 401, "No fue posible autenticar la petición con el servicio de productos");
    }

    @ExceptionHandler(ProductoTimeoutException.class)
    public ResponseEntity<GenericResponse> handleTimeoutError(ProductoTimeoutException ex) {
        return build(HttpStatus.GATEWAY_TIMEOUT, 504, "El servicio de productos no respondió a tiempo");
    }

    @ExceptionHandler(ProductoEmptyResponseException.class)
    public ResponseEntity<GenericResponse> handleEmptyResponse(ProductoEmptyResponseException ex) {
        return build(HttpStatus.BAD_GATEWAY, 502, "El servicio de productos no devolvió información");
    }

    @ExceptionHandler(ProductoIntegrationException.class)
    public ResponseEntity<GenericResponse> handleIntegrationError(ProductoIntegrationException ex) {
        return build(HttpStatus.BAD_GATEWAY, 502, "Ocurrió un error al consumir el servicio de productos");
    }

    private ResponseEntity<GenericResponse> build(HttpStatus status, Integer codigo, String mensaje) {
        GenericResponse response = new GenericResponse();
        response.setCodigo(codigo);
        response.setMensaje(mensaje);
        return new ResponseEntity<>(response, status);
    }
}