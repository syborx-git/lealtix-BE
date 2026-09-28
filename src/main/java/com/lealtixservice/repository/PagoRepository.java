package com.lealtixservice.repository;

import com.lealtixservice.entity.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {

    List<Pago> findByTurnoIdTurno(Long idTurno);

    List<Pago> findByComandaId(UUID comandaId);

    @Query("SELECT p.metodoPago, COUNT(p), SUM(p.montoCuenta), SUM(p.montoPropina) " +
           "FROM Pago p WHERE p.turno.idTurno = :idTurno AND p.estado = 'APLICADO' " +
           "GROUP BY p.metodoPago")
    List<Object[]> getDesgloseMetodosPago(@Param("idTurno") Long idTurno);

    @Query("SELECT p FROM Pago p WHERE p.tenant.id = :tenantId AND p.comanda.mesero.id = :idMesero " +
           "AND (:idTurno IS NULL OR p.turno.idTurno = :idTurno) ORDER BY p.fecha DESC")
    List<Pago> findPagosByMesero(@Param("tenantId") Long tenantId,
                                 @Param("idMesero") Long idMesero,
                                 @Param("idTurno") Long idTurno);
}
