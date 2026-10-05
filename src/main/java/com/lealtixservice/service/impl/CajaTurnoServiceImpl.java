package com.lealtixservice.service.impl;

import com.lealtixservice.dto.caja.*;
import com.lealtixservice.entity.*;
import com.lealtixservice.enums.MesaEstado;
import com.lealtixservice.enums.OrderStatus;
import com.lealtixservice.enums.PaymentMethod;
import com.lealtixservice.exception.ResourceNotFoundException;
import com.lealtixservice.repository.*;
import com.lealtixservice.service.CajaTurnoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CajaTurnoServiceImpl implements CajaTurnoService {

    private final TurnoRepository turnoRepository;
    private final PagoRepository pagoRepository;
    private final LiquidacionPropinaRepository liquidacionPropinaRepository;
    private final ClientOrderRepository clientOrderRepository;
    private final TenantUserRepository tenantUserRepository;
    private final TenantRepository tenantRepository;
    private final MesaRepository mesaRepository;
    private final com.lealtixservice.service.OrderSseService orderSseService;

    @Override
    @Transactional(readOnly = true)
    public TurnoDTO obtenerTurnoActivo(Long tenantId, Long cajeroId) {
        Optional<Turno> turnoOpt = (cajeroId != null && cajeroId > 0)
                ? turnoRepository.findFirstByTenantIdAndCajeroIdAndEstado(tenantId, cajeroId, "ABIERTO")
                : turnoRepository.findFirstByTenantIdAndEstado(tenantId, "ABIERTO");

        return turnoOpt.map(this::mapToTurnoDTO).orElse(null);
    }

    @Override
    @Transactional
    public TurnoDTO abrirTurno(AbrirTurnoRequest request) {
        log.info("Abriendo turno para cajero {} en tenant {}", request.getCajeroId(), request.getTenantId());

        boolean existeAbierto = turnoRepository.existsByTenantIdAndCajeroIdAndEstado(
                request.getTenantId(), request.getCajeroId(), "ABIERTO"
        );
        if (existeAbierto) {
            throw new IllegalArgumentException("Ya cuentas con un turno de caja abierto.");
        }

        Tenant tenant = tenantRepository.findById(request.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant no encontrado"));

        TenantUser cajero = tenantUserRepository.findByIdAndTenantId(request.getCajeroId(), request.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Cajero no encontrado"));

        Turno turno = Turno.builder()
                .tenant(tenant)
                .cajero(cajero)
                .fondoInicial(request.getFondoInicial())
                .totalIngresos(BigDecimal.ZERO)
                .totalPropinas(BigDecimal.ZERO)
                .totalEfectivoDeclarado(BigDecimal.ZERO)
                .diferenciaCaja(BigDecimal.ZERO)
                .estado("ABIERTO")
                .observaciones(request.getObservaciones())
                .fechaApertura(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        Turno guardado = turnoRepository.save(turno);
        return mapToTurnoDTO(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public ResumenTurnoCorteDTO obtenerResumenTurno(Long tenantId, Long idTurno) {
        return obtenerResumenTurno(tenantId, idTurno, null);
    }

    @Override
    @Transactional(readOnly = true)
    public ResumenTurnoCorteDTO obtenerResumenTurno(Long tenantId, Long idTurno, LocalDate fecha) {
        Turno turno = turnoRepository.findById(idTurno)
                .orElseThrow(() -> new ResourceNotFoundException("Turno no encontrado"));

        List<Pago> pagos;
        if (fecha != null) {
            LocalDateTime desde = fecha.atStartOfDay();
            LocalDateTime hasta = fecha.plusDays(1).atStartOfDay();
            pagos = pagoRepository.findPagosEnRango(tenantId, idTurno, desde, hasta);
        } else {
            pagos = pagoRepository.findByTurnoIdTurno(idTurno);
        }

        BigDecimal totalCuentaVentas = BigDecimal.ZERO;
        BigDecimal totalPropinas = BigDecimal.ZERO;
        BigDecimal totalEfectivoCobrado = BigDecimal.ZERO;
        Map<String, DesgloseMetodoPagoDTO> desgloseMap = new LinkedHashMap<>();

        for (Pago p : pagos) {
            BigDecimal cuenta = p.getMontoCuenta() != null ? p.getMontoCuenta() : BigDecimal.ZERO;
            BigDecimal propina = p.getMontoPropina() != null ? p.getMontoPropina() : BigDecimal.ZERO;
            if (propina.compareTo(BigDecimal.ZERO) == 0 && p.getComanda() != null && p.getComanda().getPropina() != null) {
                propina = p.getComanda().getPropina();
            }

            totalCuentaVentas = totalCuentaVentas.add(cuenta);
            totalPropinas = totalPropinas.add(propina);

            String metodoRaw = (p.getMetodoPago() != null ? p.getMetodoPago().trim().toUpperCase() : "EFECTIVO");
            String metodo = switch (metodoRaw) {
                case "CASH", "EFECTIVO", "DINERO" -> "EFECTIVO";
                case "CARD", "TARJETA", "MASTERCARD", "VISA", "DEBITO", "CREDITO" -> "TARJETA";
                case "TRANSFER", "TRANSFERENCIA", "SPEI" -> "TRANSFERENCIA";
                case "MIXED", "MIXTO" -> "MIXTO";
                default -> metodoRaw;
            };

            BigDecimal recaudado = cuenta.add(propina);
            if ("EFECTIVO".equalsIgnoreCase(metodo)) {
                totalEfectivoCobrado = totalEfectivoCobrado.add(recaudado);
            }

            final BigDecimal finalPropina = propina;
            desgloseMap.compute(metodo, (k, v) -> {
                if (v == null) {
                    return DesgloseMetodoPagoDTO.builder()
                            .metodoPago(k)
                            .transacciones(1L)
                            .totalCuenta(cuenta)
                            .totalPropina(finalPropina)
                            .totalRecaudado(recaudado)
                            .build();
                } else {
                    v.setTransacciones(v.getTransacciones() + 1);
                    v.setTotalCuenta(v.getTotalCuenta().add(cuenta));
                    v.setTotalPropina(v.getTotalPropina().add(finalPropina));
                    v.setTotalRecaudado(v.getTotalRecaudado().add(recaudado));
                    return v;
                }
            });
        }

        List<DesgloseMetodoPagoDTO> desgloseMetodos = new ArrayList<>(desgloseMap.values());
        desgloseMetodos.sort((a, b) -> b.getTotalRecaudado().compareTo(a.getTotalRecaudado()));

        long totalArticulos = pagos.stream()
                .map(Pago::getComanda)
                .filter(Objects::nonNull)
                .mapToLong(o -> o.getItems() != null ? o.getItems().stream().mapToLong(i -> i.getCantidad() != null ? i.getCantidad() : 1).sum() : 0)
                .sum();

        BigDecimal totalRecaudado = totalCuentaVentas.add(totalPropinas);

        // Si fecha viene especificada (corte del día), las ventas corresponden al día.
        // Si fecha es null (arqueo/cierre de turno completo), se usa el balance acumulado del turno.
        BigDecimal ventasFinal = (fecha != null) ? totalRecaudado : turno.getTotalIngresos();
        BigDecimal propinasFinal = (fecha != null) ? totalPropinas : turno.getTotalPropinas();
        BigDecimal efectivoEsperado = turno.getFondoInicial().add(totalEfectivoCobrado);

        return ResumenTurnoCorteDTO.builder()
                .turno(mapToTurnoDTO(turno))
                .totalArticulosVendidos(totalArticulos)
                .totalComandasCobradas((long) pagos.size())
                .totalVentas(ventasFinal)
                .totalCuenta(totalCuentaVentas)
                .totalPropinas(propinasFinal)
                .totalRecaudado(totalRecaudado)
                .fondoInicial(turno.getFondoInicial())
                .efectivoEsperadoEnCaja(efectivoEsperado)
                .desgloseMetodos(desgloseMetodos)
                .fechaCorte(fecha)
                .build();
    }

    @Override
    @Transactional
    public TurnoDTO cerrarTurno(CerrarTurnoRequest request) {
        log.info("Cerrando turno {} para tenant {}", request.getIdTurno(), request.getTenantId());

        Turno turno = turnoRepository.findById(request.getIdTurno())
                .orElseThrow(() -> new ResourceNotFoundException("Turno no encontrado"));

        if (!"ABIERTO".equalsIgnoreCase(turno.getEstado())) {
            throw new IllegalArgumentException("El turno ya se encuentra cerrado.");
        }

        ResumenTurnoCorteDTO resumen = obtenerResumenTurno(request.getTenantId(), request.getIdTurno());
        BigDecimal diferencia = request.getTotalEfectivoDeclarado().subtract(resumen.getEfectivoEsperadoEnCaja());

        turno.setFechaCierre(LocalDateTime.now());
        turno.setTotalEfectivoDeclarado(request.getTotalEfectivoDeclarado());
        turno.setDiferenciaCaja(diferencia);
        turno.setEstado("CERRADO");
        if (request.getObservaciones() != null && !request.getObservaciones().isBlank()) {
            turno.setObservaciones(request.getObservaciones());
        }

        Turno cerrado = turnoRepository.save(turno);
        return mapToTurnoDTO(cerrado);
    }

    @Override
    @Transactional(readOnly = true)
    public TableroCajaDTO obtenerTablero(Long tenantId) {
        List<OrderStatus> estados = List.of(
                OrderStatus.PENDIENTE,
                OrderStatus.CONFIRMADA,
                OrderStatus.EN_PREPARACION,
                OrderStatus.LISTO,
                OrderStatus.ABIERTA,
                OrderStatus.POR_COBRAR
        );

        List<ClientOrder> ordenes = clientOrderRepository.findByTenantIdAndEstadoInOrderByFechaAsc(tenantId, estados);

        List<ComandaCajaRowDTO> cuentasAbiertas = new ArrayList<>();
        List<ComandaCajaRowDTO> cuentasPorCobrar = new ArrayList<>();

        if (ordenes != null) {
            for (ClientOrder order : ordenes) {
                ComandaCajaRowDTO row = mapToComandaRowDTO(order);
                if (order.getEstado() == OrderStatus.POR_COBRAR) {
                    cuentasPorCobrar.add(row);
                } else {
                    cuentasAbiertas.add(row);
                }
            }
        }

        return TableroCajaDTO.builder()
                .cuentasAbiertas(cuentasAbiertas)
                .cuentasPorCobrar(cuentasPorCobrar)
                .build();
    }

    @Override
    @Transactional
    public TicketPrecuentaDTO imprimirTicketPrecuenta(Long tenantId, UUID orderId) {
        log.info("Imprimiendo ticket de pre-cuenta para comanda {}", orderId);

        ClientOrder order = clientOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Comanda no encontrada"));

        if (order.getEstado() != OrderStatus.PAGADA && order.getEstado() != OrderStatus.CANCELADA) {
            order.setEstado(OrderStatus.POR_COBRAR);
        }
        order.setFechaImpresionTicket(LocalDateTime.now());
        ClientOrder savedPrecuentaOrder = clientOrderRepository.save(order);
        try {
            orderSseService.publishOrderStatusChanged(com.lealtixservice.mapper.ClientOrderMapper.toDTO(savedPrecuentaOrder));
        } catch (Exception e) {
            log.warn("No se pudo notificar evento SSE pre-cuenta para comanda {}: {}", order.getId(), e.getMessage());
        }

        List<ItemPrecuentaDTO> itemsDTO = new ArrayList<>();
        if (order.getItems() != null) {
            for (ClientOrderItem item : order.getItems()) {
                BigDecimal precio = item.getPrecioUnitario() != null ? item.getPrecioUnitario() : BigDecimal.ZERO;
                int cant = item.getCantidad() != null ? item.getCantidad() : 1;
                BigDecimal totalLinea = precio.multiply(BigDecimal.valueOf(cant));

                String prodNombre = (item.getProduct() != null && item.getProduct().getNombre() != null)
                        ? item.getProduct().getNombre()
                        : "Producto";

                itemsDTO.add(ItemPrecuentaDTO.builder()
                        .nombreProducto(prodNombre)
                        .cantidad(cant)
                        .precioUnitario(precio)
                        .totalLinea(totalLinea)
                        .asientoAlias(item.getComentarios())
                        .build());
            }
        }

        BigDecimal total = order.getTotal() != null ? order.getTotal() : BigDecimal.ZERO;

        return TicketPrecuentaDTO.builder()
                .idComanda(order.getId())
                .folioComanda(order.getId().toString().substring(0, 8).toUpperCase())
                .mesaNombre(order.getMesa() != null ? order.getMesa().getNombre() : "Mesa General")
                .meseroNombre(order.getMesero() != null ? order.getMesero().getFullName() : "Mesero en Turno")
                .clienteNombre(order.getCustomer() != null ? order.getCustomer().getName() : "Venta General")
                .fechaApertura(order.getHoraApertura() != null ? order.getHoraApertura() : order.getFecha())
                .fechaImpresion(order.getFechaImpresionTicket())
                .subtotal(order.getSubtotal() != null ? order.getSubtotal() : total)
                .descuento(order.getDescuento() != null ? order.getDescuento() : BigDecimal.ZERO)
                .total(total)
                .propinaSugerida10(total.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP))
                .propinaSugerida15(total.multiply(new BigDecimal("0.15")).setScale(2, RoundingMode.HALF_UP))
                .propinaSugerida20(total.multiply(new BigDecimal("0.20")).setScale(2, RoundingMode.HALF_UP))
                .items(itemsDTO)
                .build();
    }

    @Override
    @Transactional
    public PagoDTO cobrarComanda(UUID orderId, CobrarComandaRequest request) {
        log.info("Procesando pago de comanda {} en caja por monto {}", orderId, request.getMontoCuenta());

        ClientOrder order = clientOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Comanda no encontrada"));

        if (order.getEstado() == OrderStatus.PAGADA) {
            throw new IllegalArgumentException("La comanda ya se encuentra PAGADA.");
        }

        Turno turno = turnoRepository.findFirstByTenantIdAndCajeroIdAndEstado(request.getTenantId(), request.getCajeroId(), "ABIERTO")
                .orElseGet(() -> turnoRepository.findFirstByTenantIdAndEstado(request.getTenantId(), "ABIERTO")
                        .orElseThrow(() -> new IllegalArgumentException("No hay ningún turno de caja ABIERTO para registrar el cobro.")));

        TenantUser cajero = null;
        if (request.getCajeroId() != null && request.getCajeroId() > 0) {
            cajero = tenantUserRepository.findByIdAndTenantId(request.getCajeroId(), request.getTenantId()).orElse(null);
        }
        if (cajero == null) {
            cajero = turno.getCajero();
        }
        if (cajero == null) {
            cajero = tenantUserRepository.findAllByTenantId(request.getTenantId()).stream().findFirst().orElse(null);
        }

        BigDecimal montoCuenta = request.getMontoCuenta() != null ? request.getMontoCuenta() : (order.getTotal() != null ? order.getTotal() : BigDecimal.ZERO);
        BigDecimal montoPropina = request.getMontoPropina() != null ? request.getMontoPropina() : BigDecimal.ZERO;
        BigDecimal montoTotal = montoCuenta.add(montoPropina);

        String rawMetodo = (request.getMetodoPago() != null ? request.getMetodoPago().trim().toUpperCase() : "EFECTIVO");
        PaymentMethod paymentMethod = switch (rawMetodo) {
            case "CARD", "TARJETA", "VISA", "MASTERCARD", "DEBITO", "CREDITO" -> PaymentMethod.CARD;
            case "TRANSFER", "TRANSFERENCIA", "SPEI" -> PaymentMethod.TRANSFER;
            case "MIXED", "MIXTO", "VALES", "OTRO" -> PaymentMethod.MIXED;
            default -> PaymentMethod.CASH;
        };

        Pago pago = Pago.builder()
                .tenant(order.getTenant())
                .comanda(order)
                .turno(turno)
                .cajero(cajero)
                .metodoPago(rawMetodo)
                .montoCuenta(montoCuenta)
                .montoPropina(montoPropina)
                .montoTotal(montoTotal)
                .referencia(request.getReferencia())
                .fecha(LocalDateTime.now())
                .estado("APLICADO")
                .build();

        Pago pagoGuardado = pagoRepository.save(pago);

        turno.setTotalIngresos(turno.getTotalIngresos().add(montoCuenta));
        turno.setTotalPropinas(turno.getTotalPropinas().add(montoPropina));
        turnoRepository.save(turno);

        order.setEstado(OrderStatus.PAGADA);
        order.setTurno(turno);
        order.setTotal(montoCuenta);
        order.setPropina(montoPropina);
        order.setPaidAt(LocalDateTime.now());
        order.setPaymentReference(request.getReferencia());
        order.setPaidMethod(paymentMethod);

        if (order.getMesa() != null) {
            Mesa mesa = order.getMesa();
            mesa.setEstado(MesaEstado.LIBRE);
            mesaRepository.save(mesa);
        }

        ClientOrder savedOrder = clientOrderRepository.save(order);
        try {
            orderSseService.publishOrderStatusChanged(com.lealtixservice.mapper.ClientOrderMapper.toDTO(savedOrder));
        } catch (Exception e) {
            log.warn("No se pudo notificar evento SSE al cobrar comanda {}: {}", order.getId(), e.getMessage());
        }

        return PagoDTO.builder()
                .idPago(pagoGuardado.getIdPago())
                .tenantId(request.getTenantId())
                .idComanda(order.getId())
                .idTurno(turno.getIdTurno())
                .idCajero(cajero != null ? cajero.getId() : null)
                .nombreCajero(cajero != null ? cajero.getNombre() : "Cajero en Turno")
                .metodoPago(pagoGuardado.getMetodoPago())
                .montoCuenta(montoCuenta)
                .montoPropina(montoPropina)
                .montoTotal(montoTotal)
                .referencia(pagoGuardado.getReferencia())
                .fecha(pagoGuardado.getFecha())
                .estado(pagoGuardado.getEstado())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CorteMeseroDTO obtenerCorteMesero(Long tenantId, Long idMesero, Long idTurno, LocalDate fecha) {
        TenantUser mesero = tenantUserRepository.findByIdAndTenantId(idMesero, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Mesero no encontrado"));
        String email = mesero.getEmail();
        String nombre = mesero.getNombre();

        List<Pago> pagos = (fecha != null)
                ? pagoRepository.findPagosByMeseroEnRango(
                        tenantId, idMesero, email, nombre, fecha.atStartOfDay(), fecha.plusDays(1).atStartOfDay())
                : pagoRepository.findPagosByMesero(tenantId, idMesero, email, nombre, idTurno);

        BigDecimal totalVentas = BigDecimal.ZERO;
        BigDecimal totalPropinas = BigDecimal.ZERO;
        BigDecimal propinasPendientes = BigDecimal.ZERO;

        List<PagoDTO> pagosDTO = new ArrayList<>();
        Map<String, DesgloseMetodoPagoDTO> desgloseMap = new HashMap<>();

        for (Pago p : pagos) {
            BigDecimal cuenta = p.getMontoCuenta() != null ? p.getMontoCuenta() : BigDecimal.ZERO;
            BigDecimal propinaRaw = p.getMontoPropina() != null ? p.getMontoPropina() : BigDecimal.ZERO;
            if (propinaRaw.compareTo(BigDecimal.ZERO) == 0 && p.getComanda() != null && p.getComanda().getPropina() != null) {
                propinaRaw = p.getComanda().getPropina();
            }
            final BigDecimal propina = propinaRaw;

            totalVentas = totalVentas.add(cuenta);
            totalPropinas = totalPropinas.add(propina);

            if (p.getComanda() != null && !Boolean.TRUE.equals(p.getComanda().getPropinasLiquidadas())) {
                propinasPendientes = propinasPendientes.add(propina);
            }

            pagosDTO.add(PagoDTO.builder()
                    .idPago(p.getIdPago())
                    .idComanda(p.getComanda() != null ? p.getComanda().getId() : null)
                    .idTurno(p.getTurno() != null ? p.getTurno().getIdTurno() : null)
                    .metodoPago(p.getMetodoPago())
                    .montoCuenta(cuenta)
                    .montoPropina(propina)
                    .montoTotal(p.getMontoTotal())
                    .referencia(p.getReferencia())
                    .fecha(p.getFecha())
                    .estado(p.getEstado())
                    .build());

            String m = p.getMetodoPago();
            desgloseMap.compute(m, (k, v) -> {
                if (v == null) {
                    return DesgloseMetodoPagoDTO.builder()
                            .metodoPago(m)
                            .transacciones(1L)
                            .totalCuenta(cuenta)
                            .totalPropina(propina)
                            .totalRecaudado(cuenta.add(propina))
                            .build();
                } else {
                    v.setTransacciones(v.getTransacciones() + 1);
                    v.setTotalCuenta(v.getTotalCuenta().add(cuenta));
                    v.setTotalPropina(v.getTotalPropina().add(propina));
                    v.setTotalRecaudado(v.getTotalRecaudado().add(cuenta).add(propina));
                    return v;
                }
            });
        }

        return CorteMeseroDTO.builder()
                .idMesero(mesero.getId())
                .nombreMesero(mesero.getNombre())
                .totalComandasAtendidas((long) pagos.size())
                .totalVentas(totalVentas)
                .totalPropinas(totalPropinas)
                .propinasPendientesLiquidar(propinasPendientes)
                .pagosRealizados(pagosDTO)
                .desgloseMetodos(new ArrayList<>(desgloseMap.values()))
                .build();
    }

    @Override
    @Transactional
    public LiquidacionPropina liquidarPropinasMesero(LiquidarPropinasRequest request) {
        log.info("Liquidando propinas para mesero {} en turno {}", request.getIdMesero(), request.getIdTurno());

        Turno turno = turnoRepository.findById(request.getIdTurno())
                .orElseThrow(() -> new ResourceNotFoundException("Turno no encontrado"));

        TenantUser mesero = tenantUserRepository.findByIdAndTenantId(request.getIdMesero(), request.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Mesero no encontrado"));

        TenantUser cajero = tenantUserRepository.findByIdAndTenantId(request.getIdCajero(), request.getTenantId())
                .orElse(null);

        List<ClientOrder> ordenesPendientes = clientOrderRepository
                .findOrdenesConPropinasPendientes(
                        request.getTenantId(), request.getIdTurno(), request.getIdMesero(), mesero.getEmail(), mesero.getNombre()
                );

        if (ordenesPendientes.isEmpty()) {
            ordenesPendientes = clientOrderRepository
                    .findOrdenesConPropinasPendientes(
                            request.getTenantId(), null, request.getIdMesero(), mesero.getEmail(), mesero.getNombre()
                    );
        }

        BigDecimal montoBruto = ordenesPendientes.stream()
                .map(o -> {
                    if (o.getPropina() != null && o.getPropina().compareTo(BigDecimal.ZERO) > 0) {
                        return o.getPropina();
                    }
                    List<Pago> pgs = pagoRepository.findByComandaId(o.getId());
                    return pgs.stream()
                            .map(p -> p.getMontoPropina() != null ? p.getMontoPropina() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (montoBruto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("No hay propinas pendientes de liquidar para este mesero en el turno actual.");
        }

        BigDecimal porcRetencion = request.getPorcentajeRetencion() != null ? request.getPorcentajeRetencion() : BigDecimal.ZERO;
        BigDecimal montoRetencion = montoBruto.multiply(porcRetencion.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal montoNeto = montoBruto.subtract(montoRetencion);

        LiquidacionPropina liq = LiquidacionPropina.builder()
                .tenant(turno.getTenant())
                .turno(turno)
                .mesero(mesero)
                .cajero(cajero)
                .montoBruto(montoBruto)
                .porcentajeRetencion(porcRetencion)
                .montoRetencion(montoRetencion)
                .montoNetoPagado(montoNeto)
                .fechaPago(LocalDateTime.now())
                .build();

        LiquidacionPropina guardada = liquidacionPropinaRepository.save(liq);

        for (ClientOrder o : ordenesPendientes) {
            o.setPropinasLiquidadas(true);
            o.setFechaLiquidacionPropinas(LocalDateTime.now());
        }
        clientOrderRepository.saveAll(ordenesPendientes);

        return guardada;
    }

    private TurnoDTO mapToTurnoDTO(Turno t) {
        return TurnoDTO.builder()
                .idTurno(t.getIdTurno())
                .tenantId(t.getTenant().getId())
                .idCajero(t.getCajero() != null ? t.getCajero().getId() : null)
                .nombreCajero(t.getCajero() != null ? t.getCajero().getNombre() : "Cajero en Turno")
                .fechaApertura(t.getFechaApertura())
                .fechaCierre(t.getFechaCierre())
                .fondoInicial(t.getFondoInicial())
                .totalIngresos(t.getTotalIngresos())
                .totalPropinas(t.getTotalPropinas())
                .totalEfectivoDeclarado(t.getTotalEfectivoDeclarado())
                .diferenciaCaja(t.getDiferenciaCaja())
                .estado(t.getEstado())
                .observaciones(t.getObservaciones())
                .build();
    }

    private ComandaCajaRowDTO mapToComandaRowDTO(ClientOrder o) {
        int itemsCount = o.getItems() != null ? o.getItems().stream().mapToInt(i -> i.getCantidad() != null ? i.getCantidad() : 1).sum() : 0;
        String folio = "";
        if (o.getId() != null) {
            String strId = o.getId().toString();
            folio = strId.length() > 8 ? strId.substring(0, 8).toUpperCase() : strId.toUpperCase();
        }
        String meseroNom = "General";
        if (o.getMesero() != null && o.getMesero().getFullName() != null && !o.getMesero().getFullName().isBlank()) {
            meseroNom = o.getMesero().getFullName();
        }
        String clienteNom = "Venta General";
        if (o.getCustomer() != null && o.getCustomer().getName() != null && !o.getCustomer().getName().isBlank()) {
            clienteNom = o.getCustomer().getName();
        }
        return ComandaCajaRowDTO.builder()
                .id(o.getId())
                .folioComanda(folio)
                .estado(o.getEstado() != null ? o.getEstado().name() : "")
                .idMesa(o.getMesa() != null ? o.getMesa().getId() : null)
                .mesaNombre(o.getMesa() != null ? o.getMesa().getNombre() : "Mesa General")
                .idMesero(o.getMesero() != null ? o.getMesero().getId() : null)
                .meseroNombre(meseroNom)
                .clienteNombre(clienteNom)
                .subtotal(o.getSubtotal() != null ? o.getSubtotal() : (o.getTotal() != null ? o.getTotal() : BigDecimal.ZERO))
                .descuento(o.getDescuento() != null ? o.getDescuento() : BigDecimal.ZERO)
                .total(o.getTotal() != null ? o.getTotal() : BigDecimal.ZERO)
                .totalItems(itemsCount)
                .horaApertura(o.getHoraApertura() != null ? o.getHoraApertura() : o.getFecha())
                .fechaImpresionTicket(o.getFechaImpresionTicket())
                .propinasLiquidadas(Boolean.TRUE.equals(o.getPropinasLiquidadas()))
                .build();
    }
}
