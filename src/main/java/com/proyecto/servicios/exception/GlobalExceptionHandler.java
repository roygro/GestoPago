package com.proyecto.servicios.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.proyecto.servicios.exception.cliente.*;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ---------- 400: validaciones y peticiones mal formadas ----------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> manejaValidacion(MethodArgumentNotValidException ex) {
        List<ErrorResponse.CampoError> errores = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new ErrorResponse.CampoError(e.getField(), e.getDefaultMessage()))
                .collect(Collectors.toList());
        return build(HttpStatus.BAD_REQUEST,
                "Error de validación: revise los campos enviados", errores);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> manejaConstraint(ConstraintViolationException ex) {
        List<ErrorResponse.CampoError> errores = ex.getConstraintViolations().stream()
                .map(v -> new ErrorResponse.CampoError(v.getPropertyPath().toString(), v.getMessage()))
                .collect(Collectors.toList());
        return build(HttpStatus.BAD_REQUEST,
                "Error de validación: revise los datos enviados", errores);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> manejaCuerpoInvalido(HttpMessageNotReadableException ex) {
        if (ex.getCause() instanceof UnrecognizedPropertyException desconocido) {
            return build(HttpStatus.BAD_REQUEST,
                    "El campo '" + desconocido.getPropertyName()
                            + "' no es válido o no puede ser enviado por el cliente");
        }
        String detalle = "";
        if (ex.getCause() instanceof JsonMappingException mapeo && !mapeo.getPath().isEmpty()) {
            String campo = mapeo.getPath().stream()
                    .map(ref -> ref.getFieldName() != null ? ref.getFieldName() : "[" + ref.getIndex() + "]")
                    .collect(Collectors.joining("."));
            detalle = " Revise el campo '" + campo + "'.";
        }
        return build(HttpStatus.BAD_REQUEST,
                "El cuerpo de la petición es inválido o está mal formado (revise el JSON, las fechas y los tipos de dato)." + detalle);
    }

    // ---------- 404 / 405 / 415: ruta, método o formato incorrecto ----------

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> manejaRutaInexistente(NoResourceFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "El recurso solicitado no existe");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> manejaMetodoNoPermitido(HttpRequestMethodNotSupportedException ex) {
        return build(HttpStatus.METHOD_NOT_ALLOWED,
                "El método HTTP " + ex.getMethod() + " no está permitido para este endpoint");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> manejaMediaType(HttpMediaTypeNotSupportedException ex) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "El formato de la petición no es soportado, use application/json");
    }

    // ---------- Excepciones de negocio ----------

    @ExceptionHandler({CurpDuplicadaException.class, RfcDuplicadoException.class,
            CorreoDuplicadoException.class, ClienteYaRegistradoException.class})
    public ResponseEntity<ErrorResponse> manejaConflicto(ClienteBusinessException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler({ClienteNoEncontradoException.class, CuentaNoEncontradaException.class})
    public ResponseEntity<ErrorResponse> manejaNoEncontrado(ClienteBusinessException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ValidacionNegocioException.class)
    public ResponseEntity<ErrorResponse> manejaValidacionNegocio(ValidacionNegocioException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(AccesoDenegadoException.class)
    public ResponseEntity<ErrorResponse> manejaAccesoDenegado(AccesoDenegadoException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(AutenticacionException.class)
    public ResponseEntity<ErrorResponse> manejaAutenticacion(AutenticacionException ex) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    // ---------- Servicio de productos (integración externa) ----------

    @ExceptionHandler(ProductoAuthenticationException.class)
    public ResponseEntity<ErrorResponse> manejaProductoAuth(ProductoAuthenticationException ex) {
        log.warn("Fallo de autenticación con servicio de productos: {}", ex.getMessage());
        return build(HttpStatus.UNAUTHORIZED, "No fue posible autenticar la petición con el servicio de productos");
    }

    @ExceptionHandler(ProductoTimeoutException.class)
    public ResponseEntity<ErrorResponse> manejaProductoTimeout(ProductoTimeoutException ex) {
        log.warn("Timeout en servicio de productos: {}", ex.getMessage());
        return build(HttpStatus.GATEWAY_TIMEOUT, "El servicio de productos no respondió a tiempo");
    }

    @ExceptionHandler(ProductoEmptyResponseException.class)
    public ResponseEntity<ErrorResponse> manejaProductoVacio(ProductoEmptyResponseException ex) {
        return build(HttpStatus.BAD_GATEWAY, "El servicio de productos no devolvió información");
    }

    @ExceptionHandler(ProductoIntegrationException.class)
    public ResponseEntity<ErrorResponse> manejaProductoIntegracion(ProductoIntegrationException ex) {
        log.error("Error de integración con servicio de productos: {}", ex.getMessage(), ex);
        return build(HttpStatus.BAD_GATEWAY, "Ocurrió un error al consumir el servicio de productos");
    }

    // ---------- Base de datos y errores inesperados ----------

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> manejaIntegridad(DataIntegrityViolationException ex) {
        log.warn("Violación de integridad de datos: {}", ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT,
                "La operación viola una restricción de datos (posible registro duplicado)");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> manejaInesperado(Exception ex) {
        log.error("Error inesperado: {}", ex.getMessage(), ex);   // el detalle solo va al log
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error interno. Intente de nuevo más tarde");
    }

    // ---------- helpers ----------

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String mensaje) {
        return new ResponseEntity<>(new ErrorResponse(status.value(), mensaje), status);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String mensaje,
                                                List<ErrorResponse.CampoError> errores) {
        return new ResponseEntity<>(new ErrorResponse(status.value(), mensaje, errores), status);
    }
}