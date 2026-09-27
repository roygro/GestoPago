package com.proyecto.servicios.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = EdadMinimaValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface EdadMinima {

    int value() default 18;

    String message() default "El cliente debe ser mayor de edad (18 años o más)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}