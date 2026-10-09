package com.proyecto.servicios.validation;

import com.proyecto.servicios.entity.sf.EstadoCivil;
import com.proyecto.servicios.repositorys.sf.EstadoCivilRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.stream.Collectors;

public class EstadoCivilValidoValidator implements ConstraintValidator<EstadoCivilValido, String> {

    private final EstadoCivilRepository estadoCivilRepository;

    // Spring crea los validadores con inyección por constructor.
    public EstadoCivilValidoValidator(EstadoCivilRepository estadoCivilRepository) {
        this.estadoCivilRepository = estadoCivilRepository;
    }

    @Override
    public boolean isValid(String valor, ConstraintValidatorContext context) {
        // Nulo o vacío: de eso se encarga @NotBlank, para no duplicar mensajes.
        if (valor == null || valor.isBlank()) {
            return true;
        }
        if (estadoCivilRepository.existsByClaveAndActivoTrue(valor)) {
            return true;
        }

        String permitidos = estadoCivilRepository.findByActivoTrueOrderByIdAsc().stream()
                .map(EstadoCivil::getClave)
                .collect(Collectors.joining(", "));

        // Solo se interpolan claves del catálogo (datos nuestros), NUNCA el valor que mandó el usuario:
        // meterlo en la plantilla permitiría inyección de expresiones EL.
        String mensaje = "El estado civil no es válido. Valores permitidos: " + permitidos;
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(escapar(mensaje)).addConstraintViolation();
        return false;
    }

    private static String escapar(String texto) {
        return texto.replace("\\", "\\\\").replace("{", "\\{").replace("}", "\\}").replace("$", "\\$");
    }
}
