package com.proyecto.servicios.security;

import com.proyecto.servicios.exception.cliente.AccesoDenegadoException;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

@Component
public class UsuarioAutenticado {

    /** Id del cliente dueño de la sesión actual (viene del JWT validado en SesionFilter). */
    public Long obtenerClienteId() {
        RequestAttributes atributos = RequestContextHolder.getRequestAttributes();
        Object id = atributos == null ? null : atributos.getAttribute("clienteId", RequestAttributes.SCOPE_REQUEST);
        if (id instanceof Long clienteId) {
            return clienteId;
        }
        throw new AccesoDenegadoException("No fue posible identificar al cliente autenticado");
    }

    /** Lanza 403 si el recurso pertenece a un cliente distinto al autenticado. */
    public void validarPropietario(Long clienteIdDelRecurso) {
        if (!obtenerClienteId().equals(clienteIdDelRecurso)) {
            throw new AccesoDenegadoException("No tiene permiso para acceder a información de otro cliente");
        }
    }
}