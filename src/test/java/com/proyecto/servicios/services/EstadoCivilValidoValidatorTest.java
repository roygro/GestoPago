package com.proyecto.servicios.services;

import com.proyecto.servicios.entity.sf.EstadoCivil;
import com.proyecto.servicios.repositorys.sf.EstadoCivilRepository;
import com.proyecto.servicios.validation.EstadoCivilValidoValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class EstadoCivilValidoValidatorTest {

    private EstadoCivilRepository repository;
    private ConstraintValidatorContext context;
    private ConstraintValidatorContext.ConstraintViolationBuilder builder;
    private EstadoCivilValidoValidator validator;

    @BeforeEach
    void setUp() {
        repository = mock(EstadoCivilRepository.class);
        context = mock(ConstraintValidatorContext.class);
        builder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(builder);
        validator = new EstadoCivilValidoValidator(repository);
    }

    private EstadoCivil estado(String clave) {
        EstadoCivil e = new EstadoCivil();
        e.setClave(clave);
        e.setDescripcion(clave);
        e.setActivo(true);
        return e;
    }

    @Test
    void claveActivaDelCatalogo_esValida() {
        when(repository.existsByClaveAndActivoTrue("SOLTERO")).thenReturn(true);

        assertTrue(validator.isValid("SOLTERO", context));
        verifyNoInteractions(context);
    }

    @Test
    void nuloOVacio_sonValidosParaEsteValidador_loAtrapaNotBlank() {
        assertTrue(validator.isValid(null, context));
        assertTrue(validator.isValid("", context));
        assertTrue(validator.isValid("   ", context));
        verifyNoInteractions(repository);
    }

    @Test
    void claveQueNoExiste_esInvalida_yElMensajeListaLosValoresPermitidos() {
        when(repository.existsByClaveAndActivoTrue("COMPLICADO")).thenReturn(false);
        when(repository.findByActivoTrueOrderByIdAsc())
                .thenReturn(List.of(estado("SOLTERO"), estado("CASADO"), estado("VIUDO")));

        assertFalse(validator.isValid("COMPLICADO", context));

        ArgumentCaptor<String> plantilla = ArgumentCaptor.forClass(String.class);
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate(plantilla.capture());
        assertEquals("El estado civil no es válido. Valores permitidos: SOLTERO, CASADO, VIUDO", plantilla.getValue());
        verify(builder).addConstraintViolation();
    }

    @Test
    void elMensajeNuncaIncluyeLoQueEscribioElUsuario_ni_permiteInyeccionEL() {
        String malicioso = "${7*7}#{1+1}{x}";
        when(repository.existsByClaveAndActivoTrue(malicioso)).thenReturn(false);
        when(repository.findByActivoTrueOrderByIdAsc()).thenReturn(List.of(estado("SOLTERO")));

        assertFalse(validator.isValid(malicioso, context));

        ArgumentCaptor<String> plantilla = ArgumentCaptor.forClass(String.class);
        verify(context).buildConstraintViolationWithTemplate(plantilla.capture());
        assertFalse(plantilla.getValue().contains("7*7"));
        assertFalse(plantilla.getValue().contains("${"));
    }

    @Test
    void claveInactivaDelCatalogo_noSeAcepta() {
        // NO_ESPECIFICADO existe en la tabla pero con activo = false
        when(repository.existsByClaveAndActivoTrue("NO_ESPECIFICADO")).thenReturn(false);
        when(repository.findByActivoTrueOrderByIdAsc()).thenReturn(List.of(estado("SOLTERO")));

        assertFalse(validator.isValid("NO_ESPECIFICADO", context));
    }

    @Test
    void esCaseSensitive_minusculasNoPasan() {
        when(repository.existsByClaveAndActivoTrue("soltero")).thenReturn(false);
        when(repository.findByActivoTrueOrderByIdAsc()).thenReturn(List.of(estado("SOLTERO")));

        assertFalse(validator.isValid("soltero", context));
    }
}
