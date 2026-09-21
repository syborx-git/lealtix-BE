package com.lealtixservice.repository;

import com.lealtixservice.entity.Mesa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MesaRepository extends JpaRepository<Mesa, Long> {
    List<Mesa> findByTenantIdOrderByNumeroAscNombreAsc(Long tenantId);

    List<Mesa> findByTenantIdAndEstadoOrderByNumeroAsc(Long tenantId, com.lealtixservice.enums.MesaEstado estado);

    boolean existsByTenantIdAndNombre(Long tenantId, String nombre);

    boolean existsByTenantIdAndNombreAndIdNot(Long tenantId, String nombre, Long id);
}