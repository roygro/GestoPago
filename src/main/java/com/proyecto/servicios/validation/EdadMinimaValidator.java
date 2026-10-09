package com.proyecto.servicios.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;
import java.time.Period;

public class EdadMinimaValidator implements ConstraintValidator<EdadMinima, LocalDate> {

    private int edadMinima;

    @Override
    public void initialize(EdadMinima constraintAnnotation) {
        this.edadMinima = constraintAnnotation.value();
    }

    @Override
    public boolean isValid(LocalDate fechaNacimiento, ConstraintValidatorContext context) {
        // Si es nulo, dejamos que @NotNull se encargue de ese error por separado
        if (fechaNacimiento == null) {
            return true;
        }
        LocalDate hoy = LocalDate.now();
        if (fechaNacimiento.isAfter(hoy)) {
            // Fecha futura: la maneja @PastOrPresent, aquí solo evitamos un cálculo raro
            return false;
        }
        int edad = Period.between(fechaNacimiento, hoy).getYears();
        return edad >= edadMinima;
    }
}