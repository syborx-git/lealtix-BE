package com.lealtixservice.repository;

import com.lealtixservice.entity.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {
    List<Reserva> findByTenantIdOrderByFechaDesc(Long tenantId);

    List<Reserva> findByTenantIdAndEstadoOrderByFechaDesc(Long tenantId, com.lealtixservice.enums.ReservaEstado estado);

    List<Reserva> findByTenantIdAndMesaId(Long tenantId, Long mesaId);
}