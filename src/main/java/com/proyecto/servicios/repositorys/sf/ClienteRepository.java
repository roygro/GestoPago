package com.proyecto.servicios.repositorys.sf;

import com.proyecto.servicios.entity.sf.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long>,JpaSpecificationExecutor<Cliente> {

    Optional<Cliente> findByCurp(String curp);

    Optional<Cliente> findByRfc(String rfc);

    Optional<Cliente> findByCorreoElectronico(String correoElectronico);

    boolean existsByCurp(String curp);

    boolean existsByRfc(String rfc);

    boolean existsByCorreoElectronico(String correoElectronico);

    List<Cliente> findByActivoTrue();

    List<Cliente> findByFechaRegistroBetween(LocalDateTime desde, LocalDateTime hasta);

    boolean existsByCorreoElectronicoIgnoreCase(String correoElectronico);
}