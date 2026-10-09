package com.proyecto.servicios.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/** El valor debe ser una nacionalidad ACTIVA del catálogo cat_nacionalidad (Mexicano, Estadounidense...). */
@Documented
@Constraint(validatedBy = NacionalidadValidaValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface NacionalidadValida {

    String message() default "La nacionalidad no es válida. Use un valor del catálogo (consulte GET /catalogos/nacionalidades)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
