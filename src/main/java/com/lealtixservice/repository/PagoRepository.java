package com.lealtixservice.repository;

import com.lealtixservice.entity.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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

    @Query("SELECT DISTINCT p FROM Pago p " +
           "LEFT JOIN p.comanda c " +
           "WHERE p.tenant.id = :tenantId " +
           "AND (:idTurno IS NULL OR p.turno.idTurno = :idTurno) " +
           "AND (p.estado IS NULL OR p.estado = 'APLICADO' OR p.estado = 'PAGADO') " +
           "AND p.fecha >= :desde AND p.fecha < :hasta " +
           "ORDER BY p.fecha DESC")
    List<Pago> findPagosEnRango(@Param("tenantId") Long tenantId,
                                @Param("idTurno") Long idTurno,
                                @Param("desde") LocalDateTime desde,
                                @Param("hasta") LocalDateTime hasta);

    @Query("SELECT DISTINCT p FROM Pago p " +
           "LEFT JOIN p.comanda c " +
           "LEFT JOIN c.mesero m " +
           "LEFT JOIN c.paidBy pb " +
           "LEFT JOIN c.mesa mesa " +
           "LEFT JOIN p.cajero cj " +
           "WHERE p.tenant.id = :tenantId AND (" +
           "m.id = :idMesero OR " +
           "(:email IS NOT NULL AND LOWER(m.email) = LOWER(:email)) OR " +
           "(:nombre IS NOT NULL AND LOWER(m.fullName) = LOWER(:nombre)) OR " +
           "(mesa.meseroUserId = :idMesero) OR " +
           "pb.id = :idMesero OR " +
           "(:email IS NOT NULL AND LOWER(pb.email) = LOWER(:email)) OR " +
           "cj.id = :idMesero OR " +
           "(:email IS NOT NULL AND LOWER(cj.email) = LOWER(:email))" +
           ") AND (:idTurno IS NULL OR p.turno.idTurno = :idTurno) ORDER BY p.fecha DESC")
    List<Pago> findPagosByMesero(@Param("tenantId") Long tenantId,
                                 @Param("idMesero") Long idMesero,
                                 @Param("email") String email,
                                 @Param("nombre") String nombre,
                                 @Param("idTurno") Long idTurno);

    default List<Pago> findPagosByMesero(Long tenantId, Long idMesero, String email, Long idTurno) {
        return findPagosByMesero(tenantId, idMesero, email, null, idTurno);
    }

    default List<Pago> findPagosByMesero(Long tenantId, Long idMesero, Long idTurno) {
        return findPagosByMesero(tenantId, idMesero, null, null, idTurno);
    }

    /**
     * Corte de un mesero acotado a un rango temporal (corte del día).
     * Se usa rango [desde, hasta) porque {@code Pago.fecha} es un LocalDateTime
     * y la comparación por "solo la fecha" no es portable entre motores de BD.
     */
    @Query("SELECT DISTINCT p FROM Pago p " +
           "LEFT JOIN p.comanda c " +
           "LEFT JOIN c.mesero m " +
           "LEFT JOIN c.paidBy pb " +
           "LEFT JOIN c.mesa mesa " +
           "LEFT JOIN p.cajero cj " +
           "WHERE p.tenant.id = :tenantId AND (" +
           "m.id = :idMesero OR " +
           "(:email IS NOT NULL AND LOWER(m.email) = LOWER(:email)) OR " +
           "(:nombre IS NOT NULL AND LOWER(m.fullName) = LOWER(:nombre)) OR " +
           "(mesa.meseroUserId = :idMesero) OR " +
           "pb.id = :idMesero OR " +
           "(:email IS NOT NULL AND LOWER(pb.email) = LOWER(:email)) OR " +
           "cj.id = :idMesero OR " +
           "(:email IS NOT NULL AND LOWER(cj.email) = LOWER(:email))" +
           ") AND p.fecha >= :desde AND p.fecha < :hasta ORDER BY p.fecha DESC")
    List<Pago> findPagosByMeseroEnRango(@Param("tenantId") Long tenantId,
                                        @Param("idMesero") Long idMesero,
                                        @Param("email") String email,
                                        @Param("nombre") String nombre,
                                        @Param("desde") LocalDateTime desde,
                                        @Param("hasta") LocalDateTime hasta);

    default List<Pago> findPagosByMeseroEnRango(Long tenantId, Long idMesero, String email, LocalDateTime desde, LocalDateTime hasta) {
        return findPagosByMeseroEnRango(tenantId, idMesero, email, null, desde, hasta);
    }

    default List<Pago> findPagosByMeseroEnRango(Long tenantId, Long idMesero, LocalDateTime desde, LocalDateTime hasta) {
        return findPagosByMeseroEnRango(tenantId, idMesero, null, null, desde, hasta);
    }
}
