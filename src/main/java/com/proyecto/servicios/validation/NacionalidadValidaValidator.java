package com.proyecto.servicios.validation;

import com.proyecto.servicios.repositorys.sf.NacionalidadRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NacionalidadValidaValidator implements ConstraintValidator<NacionalidadValida, String> {

    private final NacionalidadRepository nacionalidadRepository;

    // Spring crea los validadores con inyección por constructor.
    public NacionalidadValidaValidator(NacionalidadRepository nacionalidadRepository) {
        this.nacionalidadRepository = nacionalidadRepository;
    }

    @Override
    public boolean isValid(String valor, ConstraintValidatorContext context) {
        // Nulo o vacío: de eso se encarga @NotBlank, para no duplicar mensajes.
        if (valor == null || valor.isBlank()) {
            return true;
        }
        return nacionalidadRepository.existsByNacionalidadAndActivoTrue(valor);
    }
}
