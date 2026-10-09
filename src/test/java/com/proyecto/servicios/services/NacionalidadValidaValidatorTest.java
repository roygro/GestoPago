package com.proyecto.servicios.services;

import com.proyecto.servicios.repositorys.sf.NacionalidadRepository;
import com.proyecto.servicios.validation.NacionalidadValidaValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class NacionalidadValidaValidatorTest {

    private NacionalidadRepository repository;
    private ConstraintValidatorContext context;
    private NacionalidadValidaValidator validator;

    @BeforeEach
    void setUp() {
        repository = mock(NacionalidadRepository.class);
        context = mock(ConstraintValidatorContext.class);
        validator = new NacionalidadValidaValidator(repository);
    }

    @Test
    void nacionalidadActivaDelCatalogo_esValida() {
        when(repository.existsByNacionalidadAndActivoTrue("Mexicano")).thenReturn(true);

        assertTrue(validator.isValid("Mexicano", context));
    }

    @Test
    void conAcento_seValidaTalCualEstaEnElCatalogo() {
        when(repository.existsByNacionalidadAndActivoTrue("Alemán")).thenReturn(true);

        assertTrue(validator.isValid("Alemán", context));
    }

    @Test
    void nuloOVacio_sonValidosParaEsteValidador_loAtrapaNotBlank() {
        assertTrue(validator.isValid(null, context));
        assertTrue(validator.isValid("", context));
        assertTrue(validator.isValid("   ", context));
        verifyNoInteractions(repository);
    }

    @Test
    void valorQueNoExiste_esInvalido() {
        when(repository.existsByNacionalidadAndActivoTrue("Marciano")).thenReturn(false);

        assertFalse(validator.isValid("Marciano", context));
    }

    @Test
    void codigoIso_noSeAcepta() {
        // Se manda "Mexicano", no "MX"
        when(repository.existsByNacionalidadAndActivoTrue("MX")).thenReturn(false);

        assertFalse(validator.isValid("MX", context));
    }

    @Test
    void esCaseSensitive_minusculasNoPasan() {
        when(repository.existsByNacionalidadAndActivoTrue("mexicano")).thenReturn(false);

        assertFalse(validator.isValid("mexicano", context));
    }

    @Test
    void valorInactivo_noSeAcepta() {
        // "No especificado" existe en la tabla pero con activo = false
        when(repository.existsByNacionalidadAndActivoTrue("No especificado")).thenReturn(false);

        assertFalse(validator.isValid("No especificado", context));
    }
}
