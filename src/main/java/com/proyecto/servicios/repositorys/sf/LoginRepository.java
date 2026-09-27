package com.proyecto.servicios.repositorys.sf;

import com.proyecto.servicios.entity.sf.Login;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoginRepository extends JpaRepository<Login, Long> {

    Optional<Login> findByUsuarioAndClienteActivoTrue(String usuario);

    Optional<Login> findByJwtToken(String jwtToken);

    List<Login> findByClienteIdAndSesionActivaTrue(Long clienteId);

    List<Login> findBySesionActivaTrue();

    Optional<Login> findTopByUsuarioOrderByFechaInicioSesionDesc(String usuario);
}