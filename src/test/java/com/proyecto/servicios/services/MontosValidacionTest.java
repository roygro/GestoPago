package com.proyecto.servicios.services;

import com.proyecto.servicios.model.cliente.ClienteActualizaRequest;
import com.proyecto.servicios.model.cliente.ClienteRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** Valida SOLO la propiedad ingresoMensual, en el alta y en la actualización. */
class MontosValidacionTest {

    private static final String DIGITOS = "El ingreso mensual debe tener máximo 10 dígitos enteros y 2 decimales";
    private static final String MAYOR_A_CERO = "El ingreso mensual debe ser mayor a cero";
    private static final String OBLIGATORIO = "El ingreso mensual es obligatorio";

    private static ValidatorFactory fabrica;
    private static Validator validador;

    @BeforeAll
    static void iniciar() {
        fabrica = Validation.buildDefaultValidatorFactory();
        validador = fabrica.getValidator();
    }

    @AfterAll
    static void cerrar() {
        fabrica.close();
    }

    private Set<String> alta(String valor) {
        BigDecimal v = valor == null ? null : new BigDecimal(valor);
        return validador.validateValue(ClienteRequest.class, "ingresoMensual", v)
                .stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
    }

    private Set<String> actualizacion(String valor) {
        BigDecimal v = valor == null ? null : new BigDecimal(valor);
        return validador.validateValue(ClienteActualizaRequest.class, "ingresoMensual", v)
                .stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
    }

    @Test
    void montosValidos_enAltaYActualizacion() {
        for (String ok : new String[]{"2000", "2000.5", "2000.50", "0.01", "9999999999.99"}) {
            assertTrue(alta(ok).isEmpty(), "alta " + ok);
            assertTrue(actualizacion(ok).isEmpty(), "actualización " + ok);
        }
    }

    @Test
    void masDeDosDecimales_seRechaza() {
        // Hibernate Validator no le quita los ceros finales a un BigDecimal: 2000.500 cuenta como 3 decimales.
        for (String malo : new String[]{"2000.567", "2000.500"}) {
            assertEquals(Set.of(DIGITOS), alta(malo), "alta " + malo);
            assertEquals(Set.of(DIGITOS), actualizacion(malo), "actualización " + malo);
        }
    }

    @Test
    void masDeDiezEnteros_seRechaza_yNoTronariaLaBase() {
        // La columna es NUMERIC(12,2): 11 enteros daría error de overflow (500) si no se validara antes.
        assertEquals(Set.of(DIGITOS), alta("10000000000"));
        assertEquals(Set.of(DIGITOS), actualizacion("10000000000"));
    }

    @Test
    void cero_yNegativos_dicenQueDebeSerMayorACero() {
        for (String malo : new String[]{"0", "0.00", "-1", "-2000.00"}) {
            assertEquals(Set.of(MAYOR_A_CERO), alta(malo), "alta " + malo);
            assertEquals(Set.of(MAYOR_A_CERO), actualizacion(malo), "actualización " + malo);
        }
    }

    @Test
    void nulo_dice_queEsObligatorio() {
        assertEquals(Set.of(OBLIGATORIO), alta(null));
        assertEquals(Set.of(OBLIGATORIO), actualizacion(null));
    }
}