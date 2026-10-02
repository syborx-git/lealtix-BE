package com.lealtixservice.repository;

import com.lealtixservice.entity.Turno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TurnoRepository extends JpaRepository<Turno, Long> {

    Optional<Turno> findFirstByTenantIdAndCajeroIdAndEstado(Long tenantId, Long cajeroId, String estado);

    Optional<Turno> findFirstByTenantIdAndEstado(Long tenantId, String estado);

    boolean existsByTenantIdAndCajeroIdAndEstado(Long tenantId, Long cajeroId, String estado);
}
