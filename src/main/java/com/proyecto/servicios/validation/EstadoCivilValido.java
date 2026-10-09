package com.proyecto.servicios.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * El valor debe ser una clave ACTIVA del catálogo cat_estado_civil.
 * El mensaje final lista los valores permitidos (se arma en el validador).
 */
@Documented
@Constraint(validatedBy = EstadoCivilValidoValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface EstadoCivilValido {

    String message() default "El estado civil no es válido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
