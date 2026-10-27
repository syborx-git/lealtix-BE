package com.lealtixservice.repository;

import com.lealtixservice.entity.Mesa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MesaRepository extends JpaRepository<Mesa, Long> {
    List<Mesa> findByTenantIdOrderByNumeroAscNombreAsc(Long tenantId);

    List<Mesa> findByTenantIdAndEstadoOrderByNumeroAsc(Long tenantId, com.lealtixservice.enums.MesaEstado estado);

    List<Mesa> findByTenantIdAndIdGrupoTemporal(Long tenantId, String idGrupoTemporal);

    List<Mesa> findByTenantIdAndAreaOrderByNumeroAscNombreAsc(Long tenantId, String area);

    boolean existsByTenantIdAndNombre(Long tenantId, String nombre);

    boolean existsByTenantIdAndNombreAndIdNot(Long tenantId, String nombre, Long id);

    boolean existsByTenantIdAndNumero(Long tenantId, Integer numero);

    boolean existsByTenantIdAndNumeroAndIdNot(Long tenantId, Integer numero, Long id);

    boolean existsByTenantIdAndIdentificador(Long tenantId, String identificador);

    boolean existsByTenantIdAndIdentificadorAndIdNot(Long tenantId, String identificador, Long id);

    boolean existsByTenantIdAndAreaAndNombre(Long tenantId, String area, String nombre);

    boolean existsByTenantIdAndAreaAndNombreAndIdNot(Long tenantId, String area, String nombre, Long id);

    boolean existsByTenantIdAndAreaAndNumero(Long tenantId, String area, Integer numero);

    boolean existsByTenantIdAndAreaAndNumeroAndIdNot(Long tenantId, String area, Integer numero, Long id);

    boolean existsByTenantIdAndAreaAndIdentificador(Long tenantId, String area, String identificador);

    boolean existsByTenantIdAndAreaAndIdentificadorAndIdNot(Long tenantId, String area, String identificador, Long id);
}