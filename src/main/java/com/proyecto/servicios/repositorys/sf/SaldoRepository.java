package com.proyecto.servicios.repositorys.sf;

import com.proyecto.servicios.entity.sf.Saldo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SaldoRepository extends JpaRepository<Saldo, Long> {

    Optional<Saldo> findByCuentaId(Long cuentaId);

    Optional<Saldo> findByCuenta_NumeroCuenta(String numeroCuenta);
}