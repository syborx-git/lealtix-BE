package com.lealtixservice.service.impl;

import com.lealtixservice.dto.GenericResponse;
import com.lealtixservice.dto.compra.*;
import com.lealtixservice.entity.Insumo;
import com.lealtixservice.entity.OrdenCompra;
import com.lealtixservice.entity.OrdenCompraItem;
import com.lealtixservice.entity.RestockHistory;
import com.lealtixservice.repository.InsumoRepository;
import com.lealtixservice.repository.OrdenCompraItemRepository;
import com.lealtixservice.repository.OrdenCompraRepository;
import com.lealtixservice.repository.RestockHistoryRepository;
import com.lealtixservice.service.InventoryService;
import com.lealtixservice.service.OrdenCompraService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrdenCompraServiceImpl implements OrdenCompraService {

    private final OrdenCompraRepository ordenCompraRepository;
    private final OrdenCompraItemRepository ordenCompraItemRepository;
    private final InsumoRepository insumoRepository;
    private final RestockHistoryRepository restockHistoryRepository;
    private final InventoryService inventoryService;

    @Override
    @Transactional(readOnly = true)
    public SugerenciaRestockResponse obtenerSugerenciaRestock(Long tenantId, Double factor) {
        double mult = (factor != null && factor > 0) ? factor : 1.5;
        List<Insumo> insumos = insumoRepository.findByTenantIdAndIsActiveTrueOrderByNombreAsc(tenantId);

        // Mapa de costo promedio histórico por insumo
        Map<Long, BigDecimal> costoPromedioMap = new HashMap<>();
        List<Object[]> rawCosts = restockHistoryRepository.sumCostoYCantidadPorInsumo(tenantId);
        if (rawCosts != null) {
            for (Object[] r : rawCosts) {
                if (r != null && r.length >= 3 && r[0] != null) {
                    Long id = ((Number) r[0]).longValue();
                    BigDecimal sumCosto = r[1] != null ? new BigDecimal(r[1].toString()) : BigDecimal.ZERO;
                    BigDecimal sumCant = r[2] != null ? new BigDecimal(r[2].toString()) : BigDecimal.ZERO;
                    if (sumCant.compareTo(BigDecimal.ZERO) > 0) {
                        costoPromedioMap.put(id, sumCosto.divide(sumCant, 2, RoundingMode.HALF_UP));
                    }
                }
            }
        }

        List<OrdenCompraItemDTO> items = new ArrayList<>();
        BigDecimal totalEstimado = BigDecimal.ZERO;

        for (Insumo insumo : insumos) {
            double stockBodega = insumo.getStockBodega() != null ? insumo.getStockBodega() : 0.0;
            double stockCocina = insumo.getStockCocina() != null ? insumo.getStockCocina() : 0.0;
            double stockBarra = insumo.getStockBarra() != null ? insumo.getStockBarra() : 0.0;
            double stockDistribuido = insumo.getStock() != null ? insumo.getStock() : (stockCocina + stockBarra);
            double stockTotal = stockBodega + stockDistribuido;
            double stockMinimo = insumo.getStockMinimo() != null ? insumo.getStockMinimo() : 0.0;

            String areaDestino = insumo.getAreaDistribucion();
            if (areaDestino == null || areaDestino.isBlank()) {
                areaDestino = insumo.isEsBebida() ? "BARRA" : "COCINA";
            } else {
                areaDestino = areaDestino.trim().toUpperCase();
            }

            boolean enAlerta = (stockTotal <= 0.0) || (stockMinimo > 0.0 && stockTotal <= stockMinimo);
            if (enAlerta) {
                double target = stockMinimo > 0.0 ? stockMinimo * mult : 10.0;
                double faltante = Math.max(0.0, target - stockTotal);

                if (faltante > 0.0) {
                    BigDecimal cantSugerida = BigDecimal.valueOf(faltante).setScale(3, RoundingMode.HALF_UP);
                    BigDecimal costoUnit = costoPromedioMap.getOrDefault(insumo.getId(), BigDecimal.ZERO);
                    BigDecimal subtotal = costoUnit.multiply(cantSugerida).setScale(2, RoundingMode.HALF_UP);
                    totalEstimado = totalEstimado.add(subtotal);

                    items.add(OrdenCompraItemDTO.builder()
                            .insumoId(insumo.getId())
                            .insumoNombre(insumo.getNombre())
                            .areaDestino(areaDestino)
                            .unidad(insumo.getUnidad() != null ? insumo.getUnidad() : "pieza")
                            .stockActual(BigDecimal.valueOf(stockTotal).setScale(3, RoundingMode.HALF_UP))
                            .stockMinimo(BigDecimal.valueOf(stockMinimo).setScale(3, RoundingMode.HALF_UP))
                            .cantidadSugerida(cantSugerida)
                            .cantidadComprada(cantSugerida)
                            .costoUnitarioEstimado(costoUnit)
                            .costoUnitarioReal(costoUnit)
                            .subtotalEstimado(subtotal)
                            .subtotalReal(subtotal)
                            .surtido(false)
                            .build());
                }
            }
        }

        String folio = generarFolio(tenantId);

        return SugerenciaRestockResponse.builder()
                .tenantId(tenantId)
                .folioSugerido(folio)
                .factor(mult)
                .totalInsumosBajos(items.size())
                .totalEstimado(totalEstimado)
                .items(items)
                .build();
    }

    @Override
    @Transactional
    public GenericResponse generarOrdenCompra(GenerarOrdenCompraRequest request) {
        if (request == null || request.getTenantId() == null) {
            return new GenericResponse(400, "El tenantId es obligatorio", null);
        }

        List<OrdenCompraItemDTO> itemDTOs = request.getItems();
        if (itemDTOs == null || itemDTOs.isEmpty()) {
            SugerenciaRestockResponse sug = obtenerSugerenciaRestock(request.getTenantId(), request.getFactor());
            itemDTOs = sug.getItems();
        }

        if (itemDTOs == null || itemDTOs.isEmpty()) {
            return new GenericResponse(400, "No hay insumos pendientes de reposición", null);
        }

        String folio = generarFolio(request.getTenantId());
        String titulo = request.getTitulo() != null && !request.getTitulo().isBlank()
                ? request.getTitulo().trim()
                : "Lista de Compras - " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        BigDecimal totalEstimado = BigDecimal.ZERO;

        OrdenCompra orden = OrdenCompra.builder()
                .tenantId(request.getTenantId())
                .folio(folio)
                .titulo(titulo)
                .estado("PENDIENTE")
                .notas(request.getNotas())
                .totalEstimado(BigDecimal.ZERO)
                .totalReal(BigDecimal.ZERO)
                .build();

        orden = ordenCompraRepository.save(orden);

        List<OrdenCompraItem> entidades = new ArrayList<>();
        for (OrdenCompraItemDTO dto : itemDTOs) {
            BigDecimal subtotal = dto.getSubtotalEstimado() != null
                    ? dto.getSubtotalEstimado()
                    : (dto.getCantidadSugerida() != null && dto.getCostoUnitarioEstimado() != null
                    ? dto.getCantidadSugerida().multiply(dto.getCostoUnitarioEstimado()).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);
            totalEstimado = totalEstimado.add(subtotal);

            entidades.add(OrdenCompraItem.builder()
                    .ordenCompra(orden)
                    .insumoId(dto.getInsumoId())
                    .insumoNombre(dto.getInsumoNombre())
                    .areaDestino(dto.getAreaDestino() != null ? dto.getAreaDestino() : "COCINA")
                    .unidad(dto.getUnidad())
                    .stockActual(dto.getStockActual())
                    .stockMinimo(dto.getStockMinimo())
                    .cantidadSugerida(dto.getCantidadSugerida())
                    .cantidadComprada(dto.getCantidadSugerida())
                    .costoUnitarioEstimado(dto.getCostoUnitarioEstimado())
                    .costoUnitarioReal(dto.getCostoUnitarioEstimado())
                    .subtotalEstimado(subtotal)
                    .subtotalReal(subtotal)
                    .surtido(false)
                    .build());
        }

        ordenCompraItemRepository.saveAll(entidades);
        orden.setTotalEstimado(totalEstimado);
        orden.setItems(entidades);
        ordenCompraRepository.save(orden);

        return new GenericResponse(201, "Orden de compra generada exitosamente", toDTO(orden));
    }

    @Override
    @Transactional(readOnly = true)
    public GenericResponse listarOrdenes(Long tenantId, String estado) {
        List<OrdenCompra> lista;
        if (estado != null && !estado.isBlank()) {
            lista = ordenCompraRepository.findByTenantIdAndEstadoOrderByCreatedAtDesc(tenantId, estado.toUpperCase());
        } else {
            lista = ordenCompraRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
        }
        List<OrdenCompraDTO> dtos = lista.stream().map(this::toDTO).toList();
        return new GenericResponse(200, "Órdenes de compra obtenidas", dtos);
    }

    @Override
    @Transactional(readOnly = true)
    public GenericResponse obtenerOrden(Long id) {
        Optional<OrdenCompra> opt = ordenCompraRepository.findById(id);
        if (opt.isEmpty()) {
            return new GenericResponse(404, "Orden de compra no encontrada", null);
        }
        return new GenericResponse(200, "Orden de compra encontrada", toDTO(opt.get()));
    }

    @Override
    @Transactional
    public GenericResponse surtirOrden(Long id, SurtirOrdenCompraRequest request) {
        Optional<OrdenCompra> opt = ordenCompraRepository.findById(id);
        if (opt.isEmpty()) {
            return new GenericResponse(404, "Orden de compra no encontrada", null);
        }

        OrdenCompra orden = opt.get();
        if ("CANCELADA".equalsIgnoreCase(orden.getEstado())) {
            return new GenericResponse(400, "No se puede surtir una orden cancelada", null);
        }

        Map<Long, SurtirItemRequest> itemsRecibidos = new HashMap<>();
        if (request != null && request.getItems() != null) {
            for (SurtirItemRequest sir : request.getItems()) {
                if (sir.getInsumoId() != null) {
                    itemsRecibidos.put(sir.getInsumoId(), sir);
                }
            }
        }

        BigDecimal totalReal = BigDecimal.ZERO;
        List<OrdenCompraItem> items = orden.getItems();

        for (OrdenCompraItem item : items) {
            SurtirItemRequest sir = itemsRecibidos.get(item.getInsumoId());
            BigDecimal cant = (sir != null && sir.getCantidad() != null)
                    ? sir.getCantidad()
                    : item.getCantidadSugerida();

            BigDecimal costoTotal = (sir != null && sir.getCostoTotal() != null)
                    ? sir.getCostoTotal()
                    : (sir != null && sir.getCantidad() != null && item.getCostoUnitarioEstimado() != null
                    ? sir.getCantidad().multiply(item.getCostoUnitarioEstimado()).setScale(2, RoundingMode.HALF_UP)
                    : item.getSubtotalEstimado());

            String area = (sir != null && sir.getAreaDestino() != null && !sir.getAreaDestino().isBlank())
                    ? sir.getAreaDestino().toUpperCase()
                    : item.getAreaDestino();

            if (cant != null && cant.compareTo(BigDecimal.ZERO) > 0) {
                aplicarRestockInsumo(item.getInsumoId(), cant.doubleValue(), costoTotal.doubleValue(), area, orden.getId());
                item.setCantidadComprada(cant);
                item.setSubtotalReal(costoTotal);
                BigDecimal cUnit = cant.compareTo(BigDecimal.ZERO) > 0
                        ? costoTotal.divide(cant, 2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
                item.setCostoUnitarioReal(cUnit);
                item.setAreaDestino(area);
                item.setSurtido(true);
                totalReal = totalReal.add(costoTotal);
            }
        }

        ordenCompraItemRepository.saveAll(items);

        orden.setEstado("SURTIDA");
        orden.setSurtidaAt(LocalDateTime.now());
        orden.setTotalReal(totalReal);
        if (request != null && request.getNotas() != null && !request.getNotas().isBlank()) {
            orden.setNotas((orden.getNotas() != null ? orden.getNotas() + " | " : "") + request.getNotas());
        }
        ordenCompraRepository.save(orden);

        // Sincronizar disponibilidad en Comandix
        inventoryService.syncProductAvailabilityByTenant(orden.getTenantId());

        return new GenericResponse(200, "Restock aplicado exitosamente directamente a las áreas", toDTO(orden));
    }

    @Override
    @Transactional
    public GenericResponse restockDirecto(RestockDirectoRequest request) {
        if (request == null || request.getTenantId() == null) {
            return new GenericResponse(400, "El tenantId es obligatorio", null);
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            return new GenericResponse(400, "No se proporcionaron insumos para el restock", null);
        }

        String folio = generarFolio(request.getTenantId());
        String titulo = request.getTitulo() != null && !request.getTitulo().isBlank()
                ? request.getTitulo().trim()
                : "Restock Directo 1-Clic - " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

        OrdenCompra orden = OrdenCompra.builder()
                .tenantId(request.getTenantId())
                .folio(folio)
                .titulo(titulo)
                .estado("SURTIDA")
                .notas(request.getNotas())
                .surtidaAt(LocalDateTime.now())
                .totalEstimado(BigDecimal.ZERO)
                .totalReal(BigDecimal.ZERO)
                .build();

        orden = ordenCompraRepository.save(orden);

        BigDecimal totalReal = BigDecimal.ZERO;
        List<OrdenCompraItem> entidades = new ArrayList<>();

        for (SurtirItemRequest sir : request.getItems()) {
            if (sir.getInsumoId() == null || sir.getCantidad() == null || sir.getCantidad().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            Insumo insumo = insumoRepository.findById(sir.getInsumoId()).orElse(null);
            if (insumo == null) continue;

            String area = sir.getAreaDestino() != null && !sir.getAreaDestino().isBlank()
                    ? sir.getAreaDestino().toUpperCase()
                    : (insumo.getAreaDistribucion() != null ? insumo.getAreaDistribucion() : (insumo.isEsBebida() ? "BARRA" : "COCINA"));

            double cant = sir.getCantidad().doubleValue();
            double costoTotal = sir.getCostoTotal() != null ? sir.getCostoTotal().doubleValue() : 0.0;
            BigDecimal subtotal = BigDecimal.valueOf(costoTotal).setScale(2, RoundingMode.HALF_UP);
            BigDecimal cUnit = cant > 0 ? subtotal.divide(BigDecimal.valueOf(cant), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

            double stockTotal = (insumo.getStockBodega() != null ? insumo.getStockBodega() : 0.0)
                    + (insumo.getStock() != null ? insumo.getStock() : 0.0);

            // Aplicar restock en vivo a su área
            aplicarRestockInsumo(insumo.getId(), cant, costoTotal, area, orden.getId());

            entidades.add(OrdenCompraItem.builder()
                    .ordenCompra(orden)
                    .insumoId(insumo.getId())
                    .insumoNombre(insumo.getNombre())
                    .areaDestino(area)
                    .unidad(insumo.getUnidad() != null ? insumo.getUnidad() : "pieza")
                    .stockActual(BigDecimal.valueOf(stockTotal).setScale(3, RoundingMode.HALF_UP))
                    .stockMinimo(BigDecimal.valueOf(insumo.getStockMinimo() != null ? insumo.getStockMinimo() : 0.0).setScale(3, RoundingMode.HALF_UP))
                    .cantidadSugerida(sir.getCantidad())
                    .cantidadComprada(sir.getCantidad())
                    .costoUnitarioEstimado(cUnit)
                    .costoUnitarioReal(cUnit)
                    .subtotalEstimado(subtotal)
                    .subtotalReal(subtotal)
                    .surtido(true)
                    .build());

            totalReal = totalReal.add(subtotal);
        }

        ordenCompraItemRepository.saveAll(entidades);
        orden.setTotalEstimado(totalReal);
        orden.setTotalReal(totalReal);
        orden.setItems(entidades);
        ordenCompraRepository.save(orden);

        // Sincronizar disponibilidad Comandix
        inventoryService.syncProductAvailabilityByTenant(request.getTenantId());

        return new GenericResponse(200, "Restock directo aplicado exitosamente a Cocina/Barra", toDTO(orden));
    }

    @Override
    @Transactional
    public GenericResponse cancelarOrden(Long id, String motivo) {
        Optional<OrdenCompra> opt = ordenCompraRepository.findById(id);
        if (opt.isEmpty()) {
            return new GenericResponse(404, "Orden de compra no encontrada", null);
        }
        OrdenCompra orden = opt.get();
        if ("SURTIDA".equalsIgnoreCase(orden.getEstado())) {
            return new GenericResponse(400, "No se puede cancelar una orden ya surtida", null);
        }
        orden.setEstado("CANCELADA");
        if (motivo != null && !motivo.isBlank()) {
            orden.setNotas((orden.getNotas() != null ? orden.getNotas() + " | Cancelación: " : "Cancelación: ") + motivo);
        }
        ordenCompraRepository.save(orden);
        return new GenericResponse(200, "Orden de compra cancelada", toDTO(orden));
    }

    /**
     * Aplica el restock directamente al área destino del insumo y genera el registro en RestockHistory.
     */
    private void aplicarRestockInsumo(Long insumoId, double cantidad, double costoTotal, String areaDestino, Long ordenCompraId) {
        Insumo insumo = insumoRepository.findById(insumoId).orElse(null);
        if (insumo == null || cantidad <= 0) return;

        String area = areaDestino != null ? areaDestino.trim().toUpperCase() : "COCINA";

        if ("COCINA".equalsIgnoreCase(area)) {
            double cocina = insumo.getStockCocina() != null ? insumo.getStockCocina() : 0.0;
            insumo.setStockCocina(redondear(cocina + cantidad));
            double dist = insumo.getStock() != null ? insumo.getStock() : 0.0;
            insumo.setStock(redondear(dist + cantidad));
        } else if ("BARRA".equalsIgnoreCase(area)) {
            double barra = insumo.getStockBarra() != null ? insumo.getStockBarra() : 0.0;
            insumo.setStockBarra(redondear(barra + cantidad));
            double dist = insumo.getStock() != null ? insumo.getStock() : 0.0;
            insumo.setStock(redondear(dist + cantidad));
        } else { // ALMACEN / BODEGA
            double bodega = insumo.getStockBodega() != null ? insumo.getStockBodega() : 0.0;
            insumo.setStockBodega(redondear(bodega + cantidad));
        }

        insumoRepository.save(insumo);

        RestockHistory history = RestockHistory.builder()
                .tenantId(insumo.getTenantId())
                .insumoId(insumo.getId())
                .insumoNombre(insumo.getNombre())
                .cantidad(cantidad)
                .costoTotal(costoTotal)
                .ordenCompraId(ordenCompraId)
                .areaDestino(area)
                .build();
        restockHistoryRepository.save(history);
    }

    private String generarFolio(Long tenantId) {
        long count = ordenCompraRepository.countByTenantId(tenantId) + 1;
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        return String.format("COM-%s-%03d", date, count);
    }

    private double redondear(double valor) {
        return Math.round(valor * 1000.0) / 1000.0;
    }

    private OrdenCompraDTO toDTO(OrdenCompra oc) {
        if (oc == null) return null;
        List<OrdenCompraItemDTO> itemsDTO = new ArrayList<>();
        if (oc.getItems() != null) {
            for (OrdenCompraItem item : oc.getItems()) {
                itemsDTO.add(OrdenCompraItemDTO.builder()
                        .id(item.getId())
                        .insumoId(item.getInsumoId())
                        .insumoNombre(item.getInsumoNombre())
                        .areaDestino(item.getAreaDestino())
                        .unidad(item.getUnidad())
                        .stockActual(item.getStockActual())
                        .stockMinimo(item.getStockMinimo())
                        .cantidadSugerida(item.getCantidadSugerida())
                        .cantidadComprada(item.getCantidadComprada())
                        .costoUnitarioEstimado(item.getCostoUnitarioEstimado())
                        .costoUnitarioReal(item.getCostoUnitarioReal())
                        .subtotalEstimado(item.getSubtotalEstimado())
                        .subtotalReal(item.getSubtotalReal())
                        .surtido(item.getSurtido())
                        .build());
            }
        }

        return OrdenCompraDTO.builder()
                .id(oc.getId())
                .tenantId(oc.getTenantId())
                .folio(oc.getFolio())
                .titulo(oc.getTitulo())
                .estado(oc.getEstado())
                .totalEstimado(oc.getTotalEstimado())
                .totalReal(oc.getTotalReal())
                .notas(oc.getNotas())
                .totalArticulos(itemsDTO.size())
                .items(itemsDTO)
                .createdAt(oc.getCreatedAt())
                .surtidaAt(oc.getSurtidaAt())
                .updatedAt(oc.getUpdatedAt())
                .build();
    }
}

