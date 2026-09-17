package com.lealtixservice.repository;

import com.lealtixservice.entity.Bebida;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BebidaRepository extends JpaRepository<Bebida, Long> {

    List<Bebida> findByTenantIdAndActivoTrueOrderByNombreAsc(Long tenantId);

    Optional<Bebida> findByIdAndTenantId(Long id, Long tenantId);
}