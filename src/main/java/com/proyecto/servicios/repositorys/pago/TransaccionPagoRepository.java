package com.proyecto.servicios.repositorys.pago;

import com.proyecto.servicios.entity.pago.TransaccionPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TransaccionPagoRepository extends JpaRepository<TransaccionPago, Long> {

    Optional<TransaccionPago> findByUpc(String upc);

    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM TransaccionPago t " +
            "WHERE t.numeroCuenta = :numeroCuenta " +
            "AND t.idServicio = :idServicio " +
            "AND t.idProducto = :idProducto " +
            "AND t.monto = :monto " +
            "AND ( (:referencia IS NOT NULL AND t.referencia = :referencia) OR (:telefono IS NOT NULL AND t.telefono = :telefono) ) " +
            "AND t.estatus IN ('EXITOSA', 'PENDIENTE_CONCILIACION', 'INICIADA') " +
            "AND t.fechaCreacion >= :desde")
    boolean existeTransaccionRecienteDuplicada(
            @Param("numeroCuenta") String numeroCuenta,
            @Param("idServicio") Integer idServicio,
            @Param("idProducto") Integer idProducto,
            @Param("monto") BigDecimal monto,
            @Param("referencia") String referencia,
            @Param("telefono") String telefono,
            @Param("desde") LocalDateTime desde);
}
