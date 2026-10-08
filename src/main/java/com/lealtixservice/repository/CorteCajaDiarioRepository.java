package com.lealtixservice.repository;

import com.lealtixservice.entity.CorteCajaDiario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CorteCajaDiarioRepository extends JpaRepository<CorteCajaDiario, Long> {

    Optional<CorteCajaDiario> findByTenantIdAndFechaCorte(Long tenantId, LocalDate fechaCorte);

    boolean existsByTenantIdAndFechaCorte(Long tenantId, LocalDate fechaCorte);

    @Query("SELECT c FROM CorteCajaDiario c WHERE c.tenant.id = :tenantId " +
           "AND c.fechaCorte >= :desde AND c.fechaCorte <= :hasta " +
           "ORDER BY c.fechaCorte DESC, c.fechaHoraRegistro DESC")
    List<CorteCajaDiario> findByTenantIdAndRangoFechas(
            @Param("tenantId") Long tenantId,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);

    List<CorteCajaDiario> findByTenantIdOrderByFechaCorteDesc(Long tenantId);
}
