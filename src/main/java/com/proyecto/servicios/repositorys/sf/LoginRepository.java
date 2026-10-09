package com.proyecto.servicios.repositorys.sf;

import com.proyecto.servicios.entity.sf.Login;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoginRepository extends JpaRepository<Login, Long> {

    Optional<Login> findByUsuarioAndClienteActivoTrue(String usuario);

    Optional<Login> findByJwtToken(String jwtToken);

    List<Login> findByClienteIdAndSesionActivaTrue(Long clienteId);

    List<Login> findBySesionActivaTrue();

    Optional<Login> findTopByUsuarioOrderByFechaInicioSesionDesc(String usuario);

    /** true si ese nombre de usuario ya pertenece a OTRO cliente. */
    boolean existsByUsuarioAndClienteIdNot(String usuario, Long clienteId);

    /**
     * Pone sesion_activa = false en las sesiones cuya última actividad + sus minutos de
     * inactividad ya pasó, o cuyo JWT ya expiró.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE login SET sesion_activa = false "
            + "WHERE sesion_activa = true "
            + "AND (ultima_actividad + (minutos_expiracion_inactividad * INTERVAL '1 minute') < :ahora "
            + "OR jwt_fecha_expiracion < :ahora)",
            nativeQuery = true)
    int cerrarSesionesVencidas(@Param("ahora") LocalDateTime ahora);
}
