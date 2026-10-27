package com.lealtixservice.service.impl;

import com.lealtixservice.dto.*;
import com.lealtixservice.entity.ClientOrder;
import com.lealtixservice.entity.ClientOrderItem;
import com.lealtixservice.entity.Insumo;
import com.lealtixservice.entity.Merma;
import com.lealtixservice.entity.ProductAdditional;
import com.lealtixservice.entity.ProductRecipe;
import com.lealtixservice.entity.ProductSubReceta;
import com.lealtixservice.entity.TenantMenuProduct;
import com.lealtixservice.exception.BusinessRuleException;
import com.lealtixservice.exception.ResourceNotFoundException;
import com.lealtixservice.repository.*;
import com.lealtixservice.service.MermaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MermaServiceImpl implements MermaService {

    private static final String TIPO_MERMA_DEFAULT = "OPERATIVA";
    private static final String UNIDAD_DEFAULT = "pieza";

    private final MermaRepository mermaRepository;
    private final ClientOrderRepository clientOrderRepository;
    private final ClientOrderItemRepository clientOrderItemRepository;
    private final ProductRecipeRepository productRecipeRepository;
    private final ProductSubRecetaRepository productSubRecetaRepository;
    private final ProductAdditionalRepository productAdditionalRepository;
    private final RestockHistoryRepository restockHistoryRepository;
    private final InsumoRepository insumoRepository;
    private final TenantMenuProductRepository productRepository;

    @Override
    @Transactional
    public GenericResponse registrarMerma(MermaRequest request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessRuleException("Selecciona al menos un insumo/producto para registrar la merma");
        }
        if (request.getOrderId() == null || request.getOrderId().isBlank()) {
            throw new BusinessRuleException("La comanda es requerida para registrar la merma por comanda");
        }
        if (request.getItems().stream().anyMatch(i -> i.getCantidad() == null || i.getCantidad() <= 0)) {
            throw new BusinessRuleException("La cantidad de cada merma debe ser mayor a 0");
        }

        UUID orderId = UUID.fromString(request.getOrderId());
        ClientOrder order = clientOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Comanda no encontrada: " + orderId));

        Long tenantId = request.getTenantId() != null ? request.getTenantId() : order.getTenant().getId();
        if (!tenantId.equals(order.getTenant().getId())) {
            throw new BusinessRuleException("La comanda no pertenece al tenant indicado");
        }

        String ticket = ticketPara(orderId);
        UUID registroId = UUID.randomUUID();
        String tipoMerma = request.getTipoMerma() != null && !request.getTipoMerma().isBlank()
                ? request.getTipoMerma() : TIPO_MERMA_DEFAULT;
        String motivo = request.getMotivo() != null && !request.getMotivo().isBlank()
                ? request.getMotivo() : null;
        Map<Long, Double> costoUnitarioPorInsumo = costoUnitarioPromedioPorInsumo(tenantId);

        List<Merma> registros = request.getItems().stream().map(item -> {
            double costoUnitario = 0.0;
            if (item.getInsumoId() != null) {
                costoUnitario = costoUnitarioPorInsumo.getOrDefault(item.getInsumoId(), 0.0);
            }
            if (costoUnitario <= 0.0 && item.getCostoUnitario() != null && item.getCostoUnitario() > 0.0) {
                costoUnitario = item.getCostoUnitario();
            }
            if (costoUnitario <= 0.0 && item.getProductoId() != null) {
                List<Insumo> directos = insumoRepository.findByTenantIdAndProductoId(tenantId, item.getProductoId());
                if (!directos.isEmpty()) {
                    costoUnitario = costoUnitarioPorInsumo.getOrDefault(directos.get(0).getId(), 0.0);
                }
            }
            double cantidad = item.getCantidad();
            return Merma.builder()
                    .tenantId(tenantId)
                    .ticket(ticket)
                    .orderId(orderId)
                    .registroId(registroId)
                    .tipoMerma(tipoMerma)
                    .categoriaMerma("COMANDADA")
                    .usuarioId(request.getUsuarioId())
                    .usuarioNombre(request.getUsuarioNombre())
                    .motivo(motivo)
                    .insumoId(item.getInsumoId())
                    .insumoNombre(item.getInsumoNombre())
                    .productoId(item.getProductoId())
                    .productoNombre(item.getProductoNombre())
                    .cantidad(cantidad)
                    .unidad(item.getUnidad() != null && !item.getUnidad().isBlank() ? item.getUnidad() : UNIDAD_DEFAULT)
                    .costoUnitario(redondear(costoUnitario))
                    .costoTotal(redondear(cantidad * costoUnitario))
                    .build();
        }).collect(Collectors.toList());

        List<Merma> guardadas = mermaRepository.saveAll(registros);

        List<MermaResponse> respuestas = guardadas.stream().map(this::toResponse).collect(Collectors.toList());
        return new GenericResponse(200, "Merma(s) registrada(s) para " + ticket, respuestas);
    }

    @Override
    @Transactional
    public GenericResponse registrarMermaAdministrativa(MermaRequest request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessRuleException("Selecciona el insumo con la cantidad a mermar");
        }
        if (request.getTenantId() == null) {
            throw new BusinessRuleException("El tenant es requerido");
        }
        MermaRequest.MermaItemRequest item = request.getItems().get(0);
        if (item.getInsumoId() == null) {
            throw new BusinessRuleException("Selecciona un insumo para registrar la merma administrativa");
        }
        if (item.getCantidad() == null || item.getCantidad() <= 0) {
            throw new BusinessRuleException("La cantidad a mermar debe ser mayor a 0");
        }

        String origen = request.getOrigen() != null ? request.getOrigen().toUpperCase() : null;
        if (origen == null || !Set.of("BODEGA", "COCINA", "BARRA").contains(origen)) {
            throw new BusinessRuleException("Indica el almacén de origen: BODEGA, COCINA o BARRA");
        }
        if (request.getMotivo() == null || request.getMotivo().isBlank()) {
            throw new BusinessRuleException("El motivo de la merma es obligatorio");
        }

        Insumo insumo = insumoRepository.findById(item.getInsumoId())
                .orElseThrow(() -> new ResourceNotFoundException("Insumo no encontrado: " + item.getInsumoId()));
        if (!request.getTenantId().equals(insumo.getTenantId())) {
            throw new BusinessRuleException("El insumo no pertenece al tenant indicado");
        }

        double cantidad = item.getCantidad();
        double bodega = insumo.getStockBodega() != null ? insumo.getStockBodega() : 0.0;
        double cocina = insumo.getStockCocina() != null ? insumo.getStockCocina() : 0.0;
        double barra = insumo.getStockBarra() != null ? insumo.getStockBarra() : 0.0;

        switch (origen) {
            case "BODEGA" -> {
                if (bodega < cantidad) {
                    throw new BusinessRuleException("Stock insuficiente en Bodega (" + redondear(bodega) + " disponible)");
                }
                insumo.setStockBodega(redondear(bodega - cantidad));
            }
            case "COCINA" -> {
                if (cocina < cantidad) {
                    throw new BusinessRuleException("Stock insuficiente en Cocina (" + redondear(cocina) + " disponible)");
                }
                insumo.setStockCocina(redondear(cocina - cantidad));
            }
            case "BARRA" -> {
                if (barra < cantidad) {
                    throw new BusinessRuleException("Stock insuficiente en Barra (" + redondear(barra) + " disponible)");
                }
                insumo.setStockBarra(redondear(barra - cantidad));
            }
        }

        // El stock distribuido (stock) que consume el POS = cocina + barra
        insumo.setStock(redondear((insumo.getStockCocina() != null ? insumo.getStockCocina() : 0.0)
                + (insumo.getStockBarra() != null ? insumo.getStockBarra() : 0.0)));

        Map<Long, Double> costoUnitarioPorInsumo = costoUnitarioPromedioPorInsumo(insumo.getTenantId());
        double costoUnitario = costoUnitarioPorInsumo.getOrDefault(insumo.getId(), 0.0);

        Merma registro = Merma.builder()
                .tenantId(insumo.getTenantId())
                .ticket(null)
                .registroId(UUID.randomUUID())
                .tipoMerma(request.getTipoMerma() != null && !request.getTipoMerma().isBlank()
                        ? request.getTipoMerma() : TIPO_MERMA_DEFAULT)
                .categoriaMerma("ADMINISTRATIVA")
                .origen(origen)
                .motivo(request.getMotivo().trim())
                .usuarioId(request.getUsuarioId())
                .usuarioNombre(request.getUsuarioNombre())
                .insumoId(insumo.getId())
                .insumoNombre(insumo.getNombre())
                .cantidad(cantidad)
                .unidad(item.getUnidad() != null && !item.getUnidad().isBlank() ? item.getUnidad() : UNIDAD_DEFAULT)
                .costoUnitario(costoUnitario)
                .costoTotal(cantidad * costoUnitario)
                .build();
        Merma guardada = mermaRepository.save(registro);

        return new GenericResponse(200, "Merma administrativa registrada", toResponse(guardada));
    }

    @Override
    @Transactional(readOnly = true)
    public GenericResponse listarPorTenant(Long tenantId) {
        List<MermaResponse> respuestas = mermaRepository.findByTenantIdOrderByFechaDesc(tenantId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return new GenericResponse(200, "Mermas obtenidas", respuestas);
    }

    @Override
    @Transactional(readOnly = true)
    public GenericResponse resolverInsumosUsados(UUID orderId) {
        ClientOrder order = clientOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Comanda no encontrada: " + orderId));

        List<ClientOrderItem> items = clientOrderItemRepository.findByOrderId(orderId);
        Map<Long, InsumoUsadoResponse> agregados = new LinkedHashMap<>();

        for (ClientOrderItem item : items) {
            var producto = item.getProduct();
            if (producto == null) {
                continue;
            }
            List<ProductRecipe> recipes = productRecipeRepository.findByDishId(producto.getId());
            List<ProductSubReceta> subs = productSubRecetaRepository.findByDishId(producto.getId());

            if (recipes.isEmpty() && subs.isEmpty()) {
                agregarProductoDirecto(agregados, item, producto, order.getTenant().getId());
            } else {
                for (ProductRecipe r : recipes) {
                    boolean excluido = r.getModificable() != null && r.getModificable()
                            && item.getExcludedIngredientIds() != null
                            && item.getExcludedIngredientIds().contains(r.getInsumo().getId());
                    if (excluido) {
                        continue;
                    }
                    double qty = r.getCantidad() != null ? r.getCantidad().doubleValue() * item.getCantidad() : 0.0;
                    if (qty <= 0) {
                        continue;
                    }
                    agregarInsumo(agregados, r.getInsumo(), item.getProduct().getId(), item.getProduct().getNombre(), qty);
                }

                // Sub-recetas tratadas como insumos preparados
                for (ProductSubReceta s : subs) {
                    if (s.getSubReceta() == null) continue;
                    boolean excluido = s.getModificable() != null && s.getModificable()
                            && item.getExcludedIngredientIds() != null
                            && item.getExcludedIngredientIds().contains(s.getSubReceta().getId());
                    if (excluido) {
                        continue;
                    }
                    double portion = s.getCantidad() != null && s.getCantidad() > 0 ? s.getCantidad() : 1.0;
                    double qty = portion * item.getCantidad();
                    agregarSubRecetaInsumo(agregados, s.getSubReceta(), s.getUnidad(), producto.getId(), producto.getNombre(), qty, order.getTenant().getId());
                }

                if (item.getAdditionalIngredientIds() != null && !item.getAdditionalIngredientIds().isEmpty()) {
                    for (ProductAdditional a : productAdditionalRepository.findByDishId(producto.getId())) {
                        if (item.getAdditionalIngredientIds().contains(a.getInsumo().getId())) {
                            double qty = a.getCantidad() != null ? a.getCantidad().doubleValue() * item.getCantidad() : 0.0;
                            if (qty <= 0) {
                                continue;
                            }
                            agregarInsumo(agregados, a.getInsumo(), item.getProduct().getId(), item.getProduct().getNombre(), qty);
                        }
                    }
                }
            }
        }

        Map<Long, Double> costos = costoUnitarioPromedioPorInsumo(order.getTenant().getId());
        List<InsumoUsadoResponse> resultado = agregados.values().stream()
                .map(r -> {
                    double costoUnitario = 0.0;
                    if (r.getInsumoId() != null) {
                        costoUnitario = costos.getOrDefault(r.getInsumoId(), 0.0);
                        if (costoUnitario <= 0.0) {
                            Insumo ins = insumoRepository.findById(r.getInsumoId()).orElse(null);
                            if (ins != null && ins.isEsSubReceta() && ins.getProductoId() != null) {
                                TenantMenuProduct sr = productRepository.findById(ins.getProductoId()).orElse(null);
                                if (sr != null) {
                                    costoUnitario = calcularCostoSubReceta(sr, costos, order.getTenant().getId());
                                }
                            }
                        }
                    }
                    if (costoUnitario <= 0.0 && r.getProductoId() != null) {
                        List<Insumo> directos = insumoRepository.findByTenantIdAndProductoId(order.getTenant().getId(), r.getProductoId());
                        if (!directos.isEmpty()) {
                            costoUnitario = costos.getOrDefault(directos.get(0).getId(), 0.0);
                        }
                    }
                    r.setCostoUnitario(redondear(costoUnitario));
                    r.setCostoTotal(redondear(r.getCantidad() * costoUnitario));
                    return r;
                })
                .collect(Collectors.toList());

        return new GenericResponse(200, "Insumos consumidos por la comanda", resultado);
    }

    @Override
    @Transactional(readOnly = true)
    public GenericResponse resolverDetalleComandaMerma(UUID orderId) {
        ClientOrder order = clientOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Comanda no encontrada: " + orderId));

        List<ClientOrderItem> items = clientOrderItemRepository.findByOrderId(orderId);
        Map<Long, Double> costos = costoUnitarioPromedioPorInsumo(order.getTenant().getId());

        List<ComandaItemMermaDTO> itemsDTO = new ArrayList<>();

        for (ClientOrderItem item : items) {
            var producto = item.getProduct();
            if (producto == null) {
                continue;
            }

            int itemCantidad = item.getCantidad() != null ? item.getCantidad() : 1;
            double precioUnitario = item.getPrecioUnitario() != null
                    ? item.getPrecioUnitario().doubleValue()
                    : (producto.getPrecio() != null ? producto.getPrecio().doubleValue() : 0.0);
            double subtotal = redondear(precioUnitario * itemCantidad);
            String categoria = producto.getCategory() != null ? producto.getCategory().getNombre() : "General";

            List<ProductRecipe> recipes = productRecipeRepository.findByDishId(producto.getId());
            List<ProductSubReceta> subs = productSubRecetaRepository.findByDishId(producto.getId());
            List<InsumoUsadoResponse> insumos = new ArrayList<>();
            double itemCostoEstimado = 0.0;

            if (recipes.isEmpty() && subs.isEmpty()) {
                InsumoUsadoResponse r = new InsumoUsadoResponse();
                r.setProductoId(producto.getId());
                r.setProductoNombre(producto.getNombre());
                r.setCantidad((double) itemCantidad);
                r.setUnidad(producto.getUnidad() != null && !producto.getUnidad().isBlank() ? producto.getUnidad() : UNIDAD_DEFAULT);

                List<Insumo> directos = insumoRepository.findByTenantIdAndProductoId(order.getTenant().getId(), producto.getId());
                double costoUnitario = 0.0;
                if (!directos.isEmpty()) {
                    Insumo direct = directos.get(0);
                    r.setInsumoId(direct.getId());
                    r.setInsumoNombre(direct.getNombre());
                    if (direct.getUnidad() != null && !direct.getUnidad().isBlank()) {
                        r.setUnidad(direct.getUnidad());
                    }
                    costoUnitario = costos.getOrDefault(direct.getId(), 0.0);
                }

                if (costoUnitario <= 0.0 && precioUnitario > 0.0) {
                    costoUnitario = redondear(precioUnitario * 0.35);
                }

                double costoTotal = redondear(itemCantidad * costoUnitario);
                r.setCostoUnitario(redondear(costoUnitario));
                r.setCostoTotal(costoTotal);
                itemCostoEstimado = costoTotal;
                insumos.add(r);
            } else {
                for (ProductRecipe r : recipes) {
                    boolean excluido = r.getModificable() != null && r.getModificable()
                            && item.getExcludedIngredientIds() != null
                            && item.getExcludedIngredientIds().contains(r.getInsumo().getId());
                    if (excluido) {
                        continue;
                    }
                    double qty = r.getCantidad() != null ? r.getCantidad().doubleValue() * itemCantidad : 0.0;
                    if (qty <= 0) {
                        continue;
                    }

                    double costoUnitario = costos.getOrDefault(r.getInsumo().getId(), 0.0);
                    double costoTotal = redondear(qty * costoUnitario);
                    itemCostoEstimado += costoTotal;

                    InsumoUsadoResponse ins = new InsumoUsadoResponse();
                    ins.setInsumoId(r.getInsumo().getId());
                    ins.setInsumoNombre(r.getInsumo().getNombre());
                    ins.setProductoId(producto.getId());
                    ins.setProductoNombre(producto.getNombre());
                    ins.setCantidad(redondear(qty));
                    ins.setUnidad(r.getInsumo().getUnidad() != null && !r.getInsumo().getUnidad().isBlank() ? r.getInsumo().getUnidad() : UNIDAD_DEFAULT);
                    ins.setCostoUnitario(redondear(costoUnitario));
                    ins.setCostoTotal(costoTotal);
                    insumos.add(ins);
                }

                // Sub-recetas como insumos preparados
                for (ProductSubReceta s : subs) {
                    if (s.getSubReceta() == null) continue;
                    boolean excluido = s.getModificable() != null && s.getModificable()
                            && item.getExcludedIngredientIds() != null
                            && item.getExcludedIngredientIds().contains(s.getSubReceta().getId());
                    if (excluido) {
                        continue;
                    }
                    double portion = s.getCantidad() != null && s.getCantidad() > 0 ? s.getCantidad() : 1.0;
                    double qty = portion * itemCantidad;
                    double costoUnitario = calcularCostoSubReceta(s.getSubReceta(), costos, order.getTenant().getId());
                    double costoTotal = redondear(qty * costoUnitario);
                    itemCostoEstimado += costoTotal;

                    InsumoUsadoResponse ins = new InsumoUsadoResponse();
                    List<Insumo> directos = insumoRepository.findByTenantIdAndProductoId(order.getTenant().getId(), s.getSubReceta().getId());
                    if (!directos.isEmpty()) {
                        ins.setInsumoId(directos.get(0).getId());
                    } else {
                        ins.setInsumoId(s.getSubReceta().getId());
                    }
                    ins.setInsumoNombre(s.getSubReceta().getNombre());
                    ins.setProductoId(producto.getId());
                    ins.setProductoNombre(producto.getNombre());
                    ins.setCantidad(redondear(qty));
                    ins.setUnidad(s.getUnidad() != null && !s.getUnidad().isBlank() ? s.getUnidad() : (s.getSubReceta().getUnidad() != null ? s.getSubReceta().getUnidad() : UNIDAD_DEFAULT));
                    ins.setCostoUnitario(redondear(costoUnitario));
                    ins.setCostoTotal(costoTotal);
                    insumos.add(ins);
                }

                if (item.getAdditionalIngredientIds() != null && !item.getAdditionalIngredientIds().isEmpty()) {
                    for (ProductAdditional a : productAdditionalRepository.findByDishId(producto.getId())) {
                        if (item.getAdditionalIngredientIds().contains(a.getInsumo().getId())) {
                            double qty = a.getCantidad() != null ? a.getCantidad().doubleValue() * itemCantidad : 0.0;
                            if (qty <= 0) {
                                continue;
                            }
                            double costoUnitario = costos.getOrDefault(a.getInsumo().getId(), 0.0);
                            double costoTotal = redondear(qty * costoUnitario);
                            itemCostoEstimado += costoTotal;

                            InsumoUsadoResponse ins = new InsumoUsadoResponse();
                            ins.setInsumoId(a.getInsumo().getId());
                            ins.setInsumoNombre(a.getInsumo().getNombre());
                            ins.setProductoId(producto.getId());
                            ins.setProductoNombre(producto.getNombre());
                            ins.setCantidad(redondear(qty));
                            ins.setUnidad(a.getInsumo().getUnidad() != null && !a.getInsumo().getUnidad().isBlank() ? a.getInsumo().getUnidad() : UNIDAD_DEFAULT);
                            ins.setCostoUnitario(redondear(costoUnitario));
                            ins.setCostoTotal(costoTotal);
                            insumos.add(ins);
                        }
                    }
                }
            }

            ComandaItemMermaDTO itemDTO = ComandaItemMermaDTO.builder()
                    .itemId(item.getId())
                    .productoId(producto.getId())
                    .productoNombre(producto.getNombre())
                    .categoria(categoria)
                    .cantidad(itemCantidad)
                    .precioUnitario(precioUnitario)
                    .subtotal(subtotal)
                    .costoEstimado(redondear(itemCostoEstimado))
                    .tieneReceta(!recipes.isEmpty() || !subs.isEmpty())
                    .insumos(insumos)
                    .build();

            itemsDTO.add(itemDTO);
        }

        String customerName = "Venta General";
        if (order.getCustomer() != null && order.getCustomer().getName() != null) {
            customerName = order.getCustomer().getName();
        } else if (order.getClienteMesa() != null && order.getClienteMesa().getName() != null) {
            customerName = order.getClienteMesa().getName();
        }

        String mesaNombre = "Para llevar";
        if (order.getMesa() != null) {
            if (order.getMesa().getNombre() != null && !order.getMesa().getNombre().isBlank()) {
                mesaNombre = order.getMesa().getNombre();
            } else if (order.getMesa().getIdentificador() != null && !order.getMesa().getIdentificador().isBlank()) {
                mesaNombre = order.getMesa().getIdentificador();
            } else if (order.getMesa().getNumero() != null) {
                mesaNombre = "Mesa " + order.getMesa().getNumero();
            }
        }

        ComandaDetalleMermaResponse response = ComandaDetalleMermaResponse.builder()
                .orderId(order.getId())
                .ticket(ticketPara(order.getId()))
                .customerName(customerName)
                .mesaNombre(mesaNombre)
                .estado(order.getEstado() != null ? order.getEstado().name() : "")
                .totalFinal(order.getTotal() != null ? redondear(order.getTotal().doubleValue()) : 0.0)
                .items(itemsDTO)
                .build();

        return new GenericResponse(200, "Detalle de comanda para mermas", response);
    }

    /* ==================== Utilidades ==================== */

    /** Receta efectiva de un producto: sus insumos directos (las sub-recetas se tratan como insumos propios). */
    private List<ProductRecipe> effectiveRecipes(Long productId) {
        return new ArrayList<>(productRecipeRepository.findByDishId(productId));
    }

    private double calcularCostoSubReceta(com.lealtixservice.entity.TenantMenuProduct subReceta, Map<Long, Double> costos, Long tenantId) {
        if (subReceta == null) return 0.0;
        List<ProductRecipe> lines = productRecipeRepository.findByDishId(subReceta.getId());
        if (!lines.isEmpty()) {
            double totalCost = 0.0;
            for (ProductRecipe r : lines) {
                if (r.getInsumo() == null) continue;
                double qty = r.getCantidad() != null ? r.getCantidad().doubleValue() : 0.0;
                double unitCost = costos.getOrDefault(r.getInsumo().getId(), 0.0);
                totalCost += (qty * unitCost);
            }
            double rendimiento = (subReceta.getTamanoLote() != null && subReceta.getTamanoLote() > 0)
                    ? subReceta.getTamanoLote()
                    : 1.0;
            if (totalCost > 0) {
                return totalCost / rendimiento;
            }
        }
        List<Insumo> insumos = insumoRepository.findByTenantIdAndProductoId(tenantId, subReceta.getId());
        if (!insumos.isEmpty()) {
            return costos.getOrDefault(insumos.get(0).getId(), 0.0);
        }
        return 0.0;
    }

    private void agregarSubRecetaInsumo(Map<Long, InsumoUsadoResponse> agregados,
                                       com.lealtixservice.entity.TenantMenuProduct subReceta, String unidadRel,
                                       Long productoId, String productoNombre, double qty, Long tenantId) {
        List<Insumo> directos = insumoRepository.findByTenantIdAndProductoId(tenantId, subReceta.getId());
        Long insumoId = !directos.isEmpty() ? directos.get(0).getId() : subReceta.getId();
        String unidad = (unidadRel != null && !unidadRel.isBlank()) ? unidadRel : (subReceta.getUnidad() != null ? subReceta.getUnidad() : UNIDAD_DEFAULT);

        InsumoUsadoResponse entry = agregados.computeIfAbsent(insumoId, k -> {
            InsumoUsadoResponse r = new InsumoUsadoResponse();
            r.setInsumoId(insumoId);
            r.setInsumoNombre(subReceta.getNombre());
            r.setUnidad(unidad);
            r.setCantidad(0.0);
            return r;
        });
        if (entry.getProductoId() == null) {
            entry.setProductoId(productoId);
            entry.setProductoNombre(productoNombre);
        }
        entry.setCantidad(entry.getCantidad() + qty);
    }

    private void agregarInsumo(Map<Long, InsumoUsadoResponse> agregados, Insumo insumo,
                               Long productoId, String productoNombre, double qty) {
        InsumoUsadoResponse entry = agregados.computeIfAbsent(insumo.getId(), k -> {
            InsumoUsadoResponse r = new InsumoUsadoResponse();
            r.setInsumoId(insumo.getId());
            r.setInsumoNombre(insumo.getNombre());
            r.setUnidad(insumo.getUnidad() != null && !insumo.getUnidad().isBlank() ? insumo.getUnidad() : UNIDAD_DEFAULT);
            r.setCantidad(0.0);
            return r;
        });
        if (entry.getProductoId() == null) {
            entry.setProductoId(productoId);
            entry.setProductoNombre(productoNombre);
        }
        entry.setCantidad(entry.getCantidad() + qty);
    }

    private void agregarProductoDirecto(Map<Long, InsumoUsadoResponse> agregados, ClientOrderItem item,
                                        com.lealtixservice.entity.TenantMenuProduct producto, Long tenantId) {
        long key = -producto.getId();
        InsumoUsadoResponse entry = agregados.computeIfAbsent(key, k -> {
            InsumoUsadoResponse r = new InsumoUsadoResponse();
            r.setProductoId(producto.getId());
            r.setProductoNombre(producto.getNombre());
            r.setUnidad(producto.getUnidad() != null && !producto.getUnidad().isBlank() ? producto.getUnidad() : UNIDAD_DEFAULT);

            List<Insumo> directos = insumoRepository.findByTenantIdAndProductoId(tenantId, producto.getId());
            if (!directos.isEmpty()) {
                Insumo direct = directos.get(0);
                r.setInsumoId(direct.getId());
                r.setInsumoNombre(direct.getNombre());
                if (direct.getUnidad() != null && !direct.getUnidad().isBlank()) {
                    r.setUnidad(direct.getUnidad());
                }
            }

            r.setCantidad(0.0);
            return r;
        });
        entry.setCantidad(entry.getCantidad() + (item.getCantidad() != null ? item.getCantidad() : 1));
    }

    private Map<Long, Double> costoUnitarioPromedioPorInsumo(Long tenantId) {
        Map<Long, Double> costos = new HashMap<>();
        List<Object[]> filas = restockHistoryRepository.sumCostoYCantidadPorInsumo(tenantId);
        if (filas != null) {
            for (Object[] fila : filas) {
                Long insumoId = ((Number) fila[0]).longValue();
                double costoTotal = ((Number) fila[1]).doubleValue();
                double cantidad = ((Number) fila[2]).doubleValue();
                costos.put(insumoId, cantidad > 0 ? costoTotal / cantidad : 0.0);
            }
        }
        return costos;
    }

    private String ticketPara(UUID orderId) {
        return "#" + orderId.toString().substring(0, 8).toUpperCase();
    }

    private double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    private MermaResponse toResponse(Merma m) {
        MermaResponse r = new MermaResponse();
        r.setId(m.getId());
        r.setTenantId(m.getTenantId());
        r.setTicket(m.getTicket());
        r.setOrderId(m.getOrderId());
        r.setRegistroId(m.getRegistroId());
        r.setTipoMerma(m.getTipoMerma());
        r.setCategoriaMerma(m.getCategoriaMerma());
        r.setOrigen(m.getOrigen());
        r.setMotivo(m.getMotivo());
        r.setUsuarioId(m.getUsuarioId());
        r.setUsuarioNombre(m.getUsuarioNombre());
        r.setInsumoId(m.getInsumoId());
        r.setInsumoNombre(m.getInsumoNombre());
        r.setProductoId(m.getProductoId());
        r.setProductoNombre(m.getProductoNombre());
        r.setCantidad(m.getCantidad());
        r.setUnidad(m.getUnidad());
        r.setCostoUnitario(m.getCostoUnitario());
        r.setCostoTotal(m.getCostoTotal());
        r.setFecha(m.getFecha());
        return r;
    }
}