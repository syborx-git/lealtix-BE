package com.lealtixservice.repository;

import com.lealtixservice.entity.LiquidacionPropina;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LiquidacionPropinaRepository extends JpaRepository<LiquidacionPropina, Long> {

    List<LiquidacionPropina> findByTurnoIdTurno(Long idTurno);

    List<LiquidacionPropina> findByTenantIdAndMeseroIdOrderByFechaPagoDesc(Long tenantId, Long idMesero);
}
