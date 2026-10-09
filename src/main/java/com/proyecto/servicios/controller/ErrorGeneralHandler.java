package com.proyecto.servicios.controller;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.proyecto.servicios.model.GenericResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/**
 * Red de seguridad: cualquier error que no maneje ClienteExceptionHandler ni ProductoExceptionHandler
 * responde con el mismo formato { codigo, mensaje } en lugar del JSON por defecto de Spring.
 *
 * Va con la MENOR prioridad (los otros dos advices tienen la mayor) para que NUNCA tape a los
 * handlers específicos: el handler de RuntimeException de aquí atraparía también las excepciones
 * de negocio (404, 409...) si se consultara primero.
 *
 * Se limita al paquete de controllers, así no interfiere con /v3/api-docs ni con Swagger UI.
 */
@RestControllerAdvice(basePackages = "com.proyecto.servicios.controller")
@Order(Ordered.LOWEST_PRECEDENCE)
@Slf4j
public class ErrorGeneralHandler {

    /** JSON mal formado, cuerpo vacío, fecha inválida, texto donde va un número, etc. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<GenericResponse> manejaCuerpoInvalido(HttpMessageNotReadableException ex) {
        String detalle = "";
        Throwable causa = ex.getCause();
        if (causa instanceof JsonMappingException mapeo && !mapeo.getPath().isEmpty()) {
            String campo = mapeo.getPath().stream()
                    .map(ref -> ref.getFieldName() != null ? ref.getFieldName() : "[" + ref.getIndex() + "]")
                    .collect(Collectors.joining("."));
            detalle = " Revisa el campo '" + campo + "'.";
        }
        return construyeRespuesta(HttpStatus.BAD_REQUEST,
                "El cuerpo de la petición es inválido o está mal formado (revisa el JSON, las fechas y los tipos de dato)." + detalle);
    }

    /** /clientes/abc (id no numérico), fecha con formato incorrecto en /rango-fechas, etc. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<GenericResponse> manejaTipoParametro(MethodArgumentTypeMismatchException ex) {
        return construyeRespuesta(HttpStatus.BAD_REQUEST,
                "El parámetro '" + ex.getName() + "' tiene un formato inválido");
    }

    /** Falta un @RequestParam obligatorio (por ejemplo desde / hasta). */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<GenericResponse> manejaParametroFaltante(MissingServletRequestParameterException ex) {
        return construyeRespuesta(HttpStatus.BAD_REQUEST,
                "Falta el parámetro obligatorio '" + ex.getParameterName() + "'");
    }

    /** Una restricción de la BD (duplicado por concurrencia, llave foránea...) que no se validó antes. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<GenericResponse> manejaIntegridad(DataIntegrityViolationException ex) {
        log.warn("Violación de integridad de datos: {}", ex.getMostSpecificCause().getMessage());
        return construyeRespuesta(HttpStatus.CONFLICT,
                "La operación viola una restricción de datos (posible registro duplicado)");
    }

    /** Cualquier otra excepción inesperada: se registra completa en el log y al cliente solo le llega un mensaje genérico. */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<GenericResponse> manejaInesperado(RuntimeException ex) {
        log.error("Error inesperado: {}", ex.getMessage(), ex);
        return construyeRespuesta(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error interno. Intente de nuevo más tarde");
    }

    private ResponseEntity<GenericResponse> construyeRespuesta(HttpStatus status, String mensaje) {
        GenericResponse response = new GenericResponse();
        response.setCodigo(status.value());
        response.setMensaje(mensaje);
        return new ResponseEntity<>(response, status);
    }
}
