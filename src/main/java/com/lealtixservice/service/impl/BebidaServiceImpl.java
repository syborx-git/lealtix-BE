package com.lealtixservice.service.impl;

import com.lealtixservice.dto.BebidaCatalogoDTO;
import com.lealtixservice.dto.CrearBebidaRequest;
import com.lealtixservice.dto.GenericResponse;
import com.lealtixservice.entity.Bebida;
import com.lealtixservice.entity.BebidaReceta;
import com.lealtixservice.entity.Insumo;
import com.lealtixservice.exception.BusinessRuleException;
import com.lealtixservice.exception.ResourceNotFoundException;
import com.lealtixservice.repository.BebidaRecetaRepository;
import com.lealtixservice.repository.BebidaRepository;
import com.lealtixservice.repository.InsumoRepository;
import com.lealtixservice.service.BebidaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BebidaServiceImpl implements BebidaService {

    private static final Set<String> TIPOS = Set.of("directa", "preparada");
    private static final double EPS = 1e-9;

    private final BebidaRepository bebidaRepository;
    private final BebidaRecetaRepository bebidaRecetaRepository;
    private final InsumoRepository insumoRepository;

    /* ============ Catálogo POS ============ */

    @Override
    @Transactional(readOnly = true)
    public GenericResponse catalogoPos(Long tenantId) {
        List<BebidaCatalogoDTO> catalogo = new ArrayList<>();
        for (Bebida b : bebidaRepository.findByTenantIdAndActivoTrueOrderByNombreAsc(tenantId)) {
            catalogo.add(toCatalogo(b));
        }
        return new GenericResponse(200, "Catálogo de bebidas", catalogo);
    }

    /**
     * stock 'directa'   -> piezas físicas del insumo enlazado (stockBarra / 1 pieza).
     * stock 'preparada' -> min( floor(stockBarra_i / cantidad_i) ) sobre su receta.
     */
    private BebidaCatalogoDTO toCatalogo(Bebida b) {
        BebidaCatalogoDTO dto = new BebidaCatalogoDTO();
        dto.setId(b.getId());
        dto.setTenantId(b.getTenantId());
        dto.setNombre(b.getNombre());
        dto.setDescripcion(b.getDescripcion());
        dto.setPrecioVenta(b.getPrecioVenta() != null ? b.getPrecioVenta() : BigDecimal.ZERO);
        dto.setTipoBebida(b.getTipoBebida());
        dto.setUnidad(b.getUnidad() != null ? b.getUnidad() : "pieza");

        if ("preparada".equalsIgnoreCase(b.getTipoBebida())) {
            double min = Double.MAX_VALUE;
            for (BebidaReceta linea : bebidaRecetaRepository.findByBebidaIdOrderByIdAsc(b.getId())) {
                Insumo ing = insumoRepository.findById(linea.getInsumoId()).orElse(null);
                if (ing == null) continue;
                if (linea.getCantidad() == null || linea.getCantidad() <= EPS) continue;

                double barra = stockBarra(ing);
                double posibles = Math.max(0.0, Math.floor(barra / linea.getCantidad()));

                BebidaCatalogoDTO.IngredienteCatalogoDTO ingDTO = new BebidaCatalogoDTO.IngredienteCatalogoDTO();
                ingDTO.setInsumoId(ing.getId());
                ingDTO.setInsumoNombre(ing.getNombre());
                ingDTO.setUnidad(ing.getUnidad() != null ? ing.getUnidad() : "pieza");
                ingDTO.setCantidadRequerida(linea.getCantidad());
                ingDTO.setStockEnBarra(redondear(barra));
                ingDTO.setPiezasPosibles(posibles);
                dto.getIngredientes().add(ingDTO);

                if (posibles < min) min = posibles;
            }
            double disponibles = dto.getIngredientes().isEmpty() ? 0.0 : redondear(min);
            dto.setPiezasDisponibles(disponibles);
        } else {
            // directa: piezas físicas del insumo enlazado (1 pieza = 1 unidad vendida)
            double stock = 0.0;
            if (b.getInsumoId() != null) {
                Insumo fisico = insumoRepository.findById(b.getInsumoId()).orElse(null);
                if (fisico != null) stock = floorPositivo(stockBarra(fisico));
            }
            dto.setPiezasDisponibles(redondear(stock));
        }

        dto.setDisponible(dto.getPiezasDisponibles() != null && dto.getPiezasDisponibles() > 0);
        return dto;
    }

    /* ============ Inserción transaccional (bebida + receta) ============ */

    @Override
    @Transactional
    public GenericResponse crearBebida(CrearBebidaRequest request) {
        if (request == null || request.getTenantId() == null) {
            throw new BusinessRuleException("El tenant es requerido");
        }
        if (request.getNombre() == null || request.getNombre().isBlank()) {
            throw new BusinessRuleException("El nombre de la bebida es requerido");
        }
        String tipo = request.getTipoBebida() != null ? request.getTipoBebida().trim().toLowerCase() : null;
        if (tipo == null || !TIPOS.contains(tipo)) {
            throw new BusinessRuleException("El tipo de bebida debe ser 'directa' o 'preparada'");
        }
        double precio = request.getPrecioVenta() != null ? request.getPrecioVenta().doubleValue() : 0.0;
        if (precio < 0) {
            throw new BusinessRuleException("El precio de venta no puede ser negativo");
        }

        Bebida bebida = Bebida.builder()
                .tenantId(request.getTenantId())
                .nombre(request.getNombre().trim())
                .descripcion(request.getDescripcion())
                .precioVenta(BigDecimal.valueOf(precio))
                .tipoBebida(tipo)
                .unidad("pieza")
                .stockMinimo(request.getStockMinimo() != null ? request.getStockMinimo() : 0.0)
                .activo(true)
                .build();

        if ("directa".equals(tipo)) {
            // Opcional pero recomendado: enlazar el insumo físico que se descuenta 1:1
            if (request.getInsumoId() != null) {
                validarInsumoDeTenant(request.getTenantId(), request.getInsumoId());
                bebida.setInsumoId(request.getInsumoId());
            }
            bebidaRepository.save(bebida);
        } else {
            if (request.getReceta() == null || request.getReceta().isEmpty()) {
                throw new BusinessRuleException("Las bebidas preparadas requieren al menos un ingrediente en su receta");
            }
            bebidaRepository.saveAndFlush(bebida);
            for (CrearBebidaRequest.RecetaLinea linea : request.getReceta()) {
                if (linea.getInsumoId() == null || linea.getCantidad() == null || linea.getCantidad() <= 0) {
                    throw new BusinessRuleException("Cada ingrediente de la receta requiere insumo y cantidad mayor a 0");
                }
                validarInsumoDeTenant(request.getTenantId(), linea.getInsumoId());
                bebidaRecetaRepository.save(BebidaReceta.builder()
                        .bebidaId(bebida.getId())
                        .insumoId(linea.getInsumoId())
                        .cantidad(linea.getCantidad())
                        .modificable(false)
                        .build());
            }
        }

        return new GenericResponse(200, "Bebida registrada", toCatalogo(bebida));
    }

    private void validarInsumoDeTenant(Long tenantId, Long insumoId) {
        Insumo insumo = insumoRepository.findById(insumoId)
                .orElseThrow(() -> new ResourceNotFoundException("Insumo no encontrado: " + insumoId));
        if (!tenantId.equals(insumo.getTenantId())) {
            throw new BusinessRuleException("El insumo " + insumoId + " no pertenece al tenant indicado");
        }
    }

    /* ============ Helpers ============ */

    private double stockBarra(Insumo i) {
        return i.getStockBarra() != null ? i.getStockBarra() : 0.0;
    }

    private double floorPositivo(double v) {
        return v <= EPS ? 0.0 : Math.floor(v);
    }

    private double redondear(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }
}