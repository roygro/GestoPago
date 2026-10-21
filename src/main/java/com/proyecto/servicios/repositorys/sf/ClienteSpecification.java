package com.proyecto.servicios.repositorys.sf;

import com.proyecto.servicios.entity.sf.Cliente;
import com.proyecto.servicios.entity.sf.Cuenta;
import com.proyecto.servicios.model.cliente.ClienteBusquedaRequest;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class ClienteSpecification {

    private ClienteSpecification() {
    }

    /** Arma el WHERE solo con los filtros que vengan informados. */
    public static Specification<Cliente> conFiltros(ClienteBusquedaRequest f) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();

            if (f.getId() != null) {
                predicados.add(cb.equal(root.get("id"), f.getId()));
            }
            if (StringUtils.hasText(f.getCurp())) {
                predicados.add(cb.equal(root.get("curp"), f.getCurp().trim().toUpperCase()));
            }
            if (StringUtils.hasText(f.getRfc())) {
                predicados.add(cb.equal(root.get("rfc"), f.getRfc().trim().toUpperCase()));
            }
            if (StringUtils.hasText(f.getCorreoElectronico())) {
                predicados.add(cb.equal(cb.lower(root.get("correoElectronico")),
                        f.getCorreoElectronico().trim().toLowerCase()));
            }
            if (StringUtils.hasText(f.getNombre())) {
                predicados.add(cb.like(cb.lower(root.get("nombre")),
                        "%" + f.getNombre().trim().toLowerCase() + "%"));
            }
            if (StringUtils.hasText(f.getApellidoPaterno())) {
                predicados.add(cb.like(cb.lower(root.get("apellidoPaterno")),
                        "%" + f.getApellidoPaterno().trim().toLowerCase() + "%"));
            }
            if (f.getActivo() != null) {
                predicados.add(cb.equal(root.get("activo"), f.getActivo()));
            }
            if (f.getFechaRegistroDesde() != null) {
                predicados.add(cb.greaterThanOrEqualTo(
                        root.<LocalDateTime>get("fechaRegistro"), f.getFechaRegistroDesde()));
            }
            if (f.getFechaRegistroHasta() != null) {
                predicados.add(cb.lessThanOrEqualTo(
                        root.<LocalDateTime>get("fechaRegistro"), f.getFechaRegistroHasta()));
            }
            if (StringUtils.hasText(f.getNumeroCuenta())) {
                Join<Cliente, Cuenta> cuenta = root.join("cuentas");
                predicados.add(cb.equal(cuenta.get("numeroCuenta"), f.getNumeroCuenta()));
            }

            return cb.and(predicados.toArray(new Predicate[0]));
        };
    }
}