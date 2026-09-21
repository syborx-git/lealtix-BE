package com.lealtixservice.service.impl;

import com.lealtixservice.dto.dashboard.OperationalCostsDTO;
import com.lealtixservice.repository.ClientOrderRepository;
import com.lealtixservice.repository.RestockHistoryRepository;
import com.lealtixservice.repository.TenantUserRepository;
import com.lealtixservice.service.OperationalCostsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperationalCostsServiceImpl implements OperationalCostsService {

    private final RestockHistoryRepository restockHistoryRepository;
    private final TenantUserRepository tenantUserRepository;
    private final ClientOrderRepository clientOrderRepository;

    @Override
    public OperationalCostsDTO calculate(Long tenantId, int months) {
        int effectiveMonths = months <= 0 ? 2 : months;
        LocalDateTime desde = LocalDateTime.now().minusMonths(effectiveMonths);
        LocalDateTime hasta = LocalDateTime.now();

        // Costos de materia prima: total gastado en restock en el periodo
        BigDecimal costoMateriaPrima = restockHistoryRepository.sumCostoTotalSince(tenantId, desde);

        // Costos de recurso humano: suma de sueldos mensuales del equipo
        BigDecimal costoSueldos = tenantUserRepository.sumSueldoMensualByTenant(tenantId);

        long cantidadRestocks = restockHistoryRepository.findByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .filter(r -> r.getCreatedAt() != null && !r.getCreatedAt().isBefore(desde))
                .count();

        // Ventas totales del periodo
        List<Object[]> salesRows = clientOrderRepository.getSalesSummary(tenantId, desde, hasta);
        BigDecimal ventasTotales = safeDecimal(salesRows, 0);

        BigDecimal costoTotal = costoMateriaPrima.add(costoSueldos);
        BigDecimal ganancias = ventasTotales.subtract(costoTotal).max(BigDecimal.ZERO);

        double pctMP = pct(costoMateriaPrima, ventasTotales);
        double pctRH = pct(costoSueldos, ventasTotales);
        double pctTotal = pct(costoTotal, ventasTotales);
        double pctGan = Math.max(0, 100 - pctTotal);

        return new OperationalCostsDTO(
                desde, hasta,
                ventasTotales,
                costoMateriaPrima,
                costoSueldos,
                costoTotal,
                ganancias,
                pctMP, pctRH, pctTotal, pctGan,
                cantidadRestocks
        );
    }

    private BigDecimal safeDecimal(List<Object[]> rows, int index) {
        if (rows == null || rows.isEmpty() || rows.get(0) == null || rows.get(0).length <= index) {
            return BigDecimal.ZERO;
        }
        Object val = rows.get(0)[index];
        if (val == null) return BigDecimal.ZERO;
        if (val instanceof BigDecimal bd) return bd;
        return new BigDecimal(val.toString());
    }

    private double pct(BigDecimal part, BigDecimal total) {
        if (total == null || total.signum() <= 0) return 0;
        return part.multiply(BigDecimal.valueOf(100))
                .divide(total, 2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
