package com.proyecto.servicios.services;

import com.proyecto.servicios.model.cliente.ClienteRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Valida SOLO la propiedad "sexo" de ClienteRequest (validateValue no necesita un request completo
 * ni la base de datos).
 */
class ClienteRequestSexoTest {

    private static final String MENSAJE = "El sexo debe ser 'M', 'F' u 'Otros'";

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

    private Set<ConstraintViolation<ClienteRequest>> validar(String sexo) {
        return validador.validateValue(ClienteRequest.class, "sexo", sexo);
    }

    private Set<String> mensajes(String sexo) {
        return validar(sexo).stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
    }

    @Test
    void valoresPermitidos_sonM_F_yOtros() {
        assertTrue(validar("M").isEmpty());
        assertTrue(validar("F").isEmpty());
        assertTrue(validar("Otros").isEmpty());
    }

    @Test
    void laLetraO_yaNoSeAcepta() {
        assertEquals(Set.of(MENSAJE), mensajes("O"));
    }

    @Test
    void esCaseSensitive_otrosEnMinusculasOMayusculasNoPasa() {
        assertEquals(Set.of(MENSAJE), mensajes("otros"));
        assertEquals(Set.of(MENSAJE), mensajes("OTROS"));
        assertEquals(Set.of(MENSAJE), mensajes("m"));
        assertEquals(Set.of(MENSAJE), mensajes("f"));
    }

    @Test
    void otrosValoresInvalidos_dicenElMensajeExacto() {
        assertEquals(Set.of(MENSAJE), mensajes("X"));
        assertEquals(Set.of(MENSAJE), mensajes("Otro"));
        assertEquals(Set.of(MENSAJE), mensajes("Otros "));
        assertEquals(Set.of(MENSAJE), mensajes("MF"));
        assertEquals(Set.of(MENSAJE), mensajes("Masculino"));
    }

    @Test
    void nulo_dice_queElSexoEsObligatorio() {
        assertEquals(Set.of("El sexo es obligatorio"), mensajes(null));
    }

    @Test
    void vacio_dice_queEsObligatorio() {
        assertTrue(mensajes("").contains("El sexo es obligatorio"));
        assertTrue(mensajes("   ").contains("El sexo es obligatorio"));
    }
}
