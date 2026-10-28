package com.proyecto.servicios.repositorys.sf;

import com.proyecto.servicios.entity.sf.Saldo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SaldoRepository extends JpaRepository<Saldo, Long> {

    Optional<Saldo> findByCuentaId(Long cuentaId);

    Optional<Saldo> findByCuenta_NumeroCuenta(String numeroCuenta);

    /**
     * Bloqueo pesimista de escritura (SELECT ... FOR UPDATE) para garantizar consistencia
     * estricta y evitar condiciones de carrera (race conditions) bajo peticiones simultáneas.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Saldo s WHERE s.cuenta.numeroCuenta = :numeroCuenta")
    Optional<Saldo> findByCuenta_NumeroCuentaConBloqueo(@Param("numeroCuenta") String numeroCuenta);
}
