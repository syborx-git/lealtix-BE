package com.lealtixservice.service.impl;

import com.lealtixservice.dto.corte.CorteCajaDiarioDTO;
import com.lealtixservice.dto.corte.EstadoCorteDiarioHoyDTO;
import com.lealtixservice.dto.corte.GenerarCorteDiarioRequest;
import com.lealtixservice.dto.corte.ResumenCorteSistemaDTO;
import com.lealtixservice.dto.reportes.*;
import com.lealtixservice.entity.CorteCajaDiario;
import com.lealtixservice.entity.Pago;
import com.lealtixservice.entity.Tenant;
import com.lealtixservice.entity.TenantUser;
import com.lealtixservice.exception.ResourceNotFoundException;
import com.lealtixservice.repository.CorteCajaDiarioRepository;
import com.lealtixservice.repository.PagoRepository;
import com.lealtixservice.repository.TenantRepository;
import com.lealtixservice.repository.TenantUserRepository;
import com.lealtixservice.service.CorteCajaDiarioService;
import com.lealtixservice.service.ReporteExcelService;
import com.lealtixservice.util.DateRangeResolver;
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

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CorteCajaDiarioServiceImpl implements CorteCajaDiarioService {

    private static final String REPORTE_KEY = "4.2_auditoria_cortes_diarios";
    private static final String PILAR = "4. Operacion y Staff";
    private static final String NOMBRE = "Auditoría de Cortes del Día";

    private static final DateTimeFormatter FORMATO_FECHA_TABLA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_HORA_TABLA = DateTimeFormatter.ofPattern("HH:mm");

    private static final List<ColumnaDTO> COLUMNAS_METODO = List.of(
            new ColumnaDTO("metodo", "Método de Pago", TipoColumna.TEXTO, 220),
            new ColumnaDTO("sistema", "Esperado (Sistema)", TipoColumna.MONEDA, 180),
            new ColumnaDTO("real", "Declarado (Real)", TipoColumna.MONEDA, 180),
            new ColumnaDTO("diferencia", "Diferencia", TipoColumna.MONEDA, 180),
            new ColumnaDTO("estado", "Estado Cuadre", TipoColumna.TEXTO, 140)
    );

    private static final List<ColumnaDTO> COLUMNAS_DISTRIBUCION = List.of(
            new ColumnaDTO("estado", "Resultado del Cuadre", TipoColumna.TEXTO, 220),
            new ColumnaDTO("cantidad", "Cortes de Caja", TipoColumna.NUMERO, 140),
            new ColumnaDTO("pct", "% de Cortes", TipoColumna.PORCENTAJE, 140),
            new ColumnaDTO("montoDiferencia", "Impacto Neto ($)", TipoColumna.MONEDA, 180)
    );

    private static final List<ColumnaDTO> COLUMNAS_DETALLE = List.of(
            new ColumnaDTO("folio", "Folio / ID", TipoColumna.TEXTO, 120),
            new ColumnaDTO("fecha", "Fecha", TipoColumna.FECHA, 120),
            new ColumnaDTO("hora", "Hora Cierre", TipoColumna.TEXTO, 110),
            new ColumnaDTO("cajero", "Responsable / Cajero", TipoColumna.TEXTO, 220),
            new ColumnaDTO("sistema", "Total Sistema", TipoColumna.MONEDA, 150),
            new ColumnaDTO("real", "Total Real", TipoColumna.MONEDA, 150),
            new ColumnaDTO("diferencia", "Diferencia Neta", TipoColumna.MONEDA, 150),
            new ColumnaDTO("estado", "Estado", TipoColumna.TEXTO, 120),
            new ColumnaDTO("comentarios", "Comentarios / Justificación", TipoColumna.TEXTO, 320)
    );

    private final CorteCajaDiarioRepository corteCajaDiarioRepository;
    private final PagoRepository pagoRepository;
    private final TenantRepository tenantRepository;
    private final TenantUserRepository tenantUserRepository;
    private final DateRangeResolver dateRangeResolver;
    private final ReporteExcelService excelService;

    @Override
    @Transactional(readOnly = true)
    public EstadoCorteDiarioHoyDTO consultarEstadoHoy(Long tenantId) {
        LocalDate hoy = LocalDate.now();
        Optional<CorteCajaDiario> corteHoy = corteCajaDiarioRepository.findByTenantIdAndFechaCorte(tenantId, hoy);

        if (corteHoy.isPresent()) {
            return EstadoCorteDiarioHoyDTO.builder()
                    .yaGenerado(true)
                    .corte(mapToDTO(corteHoy.get()))
                    .sistemaActual(null)
                    .build();
        }

        ResumenCorteSistemaDTO resumen = calcularResumenSistemaHoy(tenantId, hoy);
        return EstadoCorteDiarioHoyDTO.builder()
                .yaGenerado(false)
                .corte(null)
                .sistemaActual(resumen)
                .build();
    }

    @Override
    public CorteCajaDiarioDTO generarCorteDiario(GenerarCorteDiarioRequest request, Long userId, String userEmail) {
        Long tenantId = request.getTenantId();
        LocalDate hoy = LocalDate.now();

        if (corteCajaDiarioRepository.existsByTenantIdAndFechaCorte(tenantId, hoy)) {
            throw new IllegalStateException("El corte del día de hoy (" + hoy + ") ya fue generado previamente. "
                    + "Por políticas contables y de auditoría no puede generarse más de una vez ni modificarse.");
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Negocio/Tenant no encontrado"));

        TenantUser cajero = null;
        if (userId != null && userId > 0) {
            cajero = tenantUserRepository.findByIdAndTenantId(userId, tenantId).orElse(null);
        }
        if (cajero == null && userEmail != null && !userEmail.isBlank()) {
            cajero = tenantUserRepository.findByEmailAndTenantId(userEmail, tenantId).orElse(null);
        }
        if (cajero == null) {
            cajero = tenantUserRepository.findAllByTenantId(tenantId).stream().findFirst().orElse(null);
        }
        if (cajero == null) {
            throw new IllegalStateException("No se encontró ningún usuario válido para firmar el corte de caja.");
        }

        ResumenCorteSistemaDTO sistema = calcularResumenSistemaHoy(tenantId, hoy);

        BigDecimal realEfectivo = request.getRealEfectivo() != null ? request.getRealEfectivo() : BigDecimal.ZERO;
        BigDecimal realTarjeta = request.getRealTarjeta() != null ? request.getRealTarjeta() : BigDecimal.ZERO;
        BigDecimal realTransferencia = request.getRealTransferencia() != null ? request.getRealTransferencia() : BigDecimal.ZERO;
        BigDecimal realOtros = request.getRealOtros() != null ? request.getRealOtros() : BigDecimal.ZERO;
        BigDecimal realTotal = realEfectivo.add(realTarjeta).add(realTransferencia).add(realOtros);

        BigDecimal diffEfectivo = realEfectivo.subtract(sistema.getSistemaEfectivo());
        BigDecimal diffTarjeta = realTarjeta.subtract(sistema.getSistemaTarjeta());
        BigDecimal diffTransferencia = realTransferencia.subtract(sistema.getSistemaTransferencia());
        BigDecimal diffOtros = realOtros.subtract(sistema.getSistemaOtros());
        BigDecimal diffTotal = realTotal.subtract(sistema.getSistemaTotal());

        String estadoDiferencia;
        if (diffTotal.compareTo(BigDecimal.ZERO) == 0) {
            estadoDiferencia = "CUADRADO";
        } else if (diffTotal.compareTo(BigDecimal.ZERO) > 0) {
            estadoDiferencia = "SOBRANTE";
        } else {
            estadoDiferencia = "FALTANTE";
        }

        String comentarios = request.getComentarios() != null ? request.getComentarios().trim() : null;

        CorteCajaDiario nuevoCorte = CorteCajaDiario.builder()
                .tenant(tenant)
                .cajero(cajero)
                .cajeroNombre(cajero.getNombre() != null ? cajero.getNombre() : cajero.getEmail())
                .cajeroEmail(cajero.getEmail())
                .fechaCorte(hoy)
                .fechaHoraRegistro(LocalDateTime.now())
                .sistemaEfectivo(sistema.getSistemaEfectivo())
                .sistemaTarjeta(sistema.getSistemaTarjeta())
                .sistemaTransferencia(sistema.getSistemaTransferencia())
                .sistemaOtros(sistema.getSistemaOtros())
                .sistemaTotal(sistema.getSistemaTotal())
                .realEfectivo(realEfectivo)
                .realTarjeta(realTarjeta)
                .realTransferencia(realTransferencia)
                .realOtros(realOtros)
                .realTotal(realTotal)
                .diferenciaEfectivo(diffEfectivo)
                .diferenciaTarjeta(diffTarjeta)
                .diferenciaTransferencia(diffTransferencia)
                .diferenciaOtros(diffOtros)
                .diferenciaTotal(diffTotal)
                .estadoDiferencia(estadoDiferencia)
                .comentarios(comentarios)
                .totalComandas(sistema.getTotalComandas())
                .totalArticulos(sistema.getTotalArticulos())
                .totalPropinas(sistema.getTotalPropinas())
                .build();

        CorteCajaDiario guardado = corteCajaDiarioRepository.save(nuevoCorte);
        log.info("Corte de caja del día {} generado exitosamente para tenant {}. Folio={}, Diferencia={}",
                hoy, tenantId, guardado.getIdCorteDiario(), diffTotal);

        return mapToDTO(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public AuditoriaCortesDiariosDTO obtenerAuditoria(Long tenantId, PresetReporte preset, LocalDateTime from, LocalDateTime to) {
        PeriodoComparativo periodo = dateRangeResolver.resolver(preset, from, to);
        return construirReporte(tenantId, periodo);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportarExcel(Long tenantId, PresetReporte preset, LocalDateTime from, LocalDateTime to) {
        PeriodoComparativo periodo = dateRangeResolver.resolver(preset, from, to);
        AuditoriaCortesDiariosDTO datos = construirReporte(tenantId, periodo);

        String subtitulo = "Periodo actual: " + periodo.actual().etiqueta()
                + " (" + periodo.actual().from().toLocalDate() + " a " + periodo.actual().to().toLocalDate() + ")"
                + " | Comparado contra: " + periodo.anterior().etiqueta()
                + " (" + periodo.anterior().from().toLocalDate() + " a " + periodo.anterior().to().toLocalDate() + ")";

        List<HojaExcelDTO> hojas = new ArrayList<>();

        // 1. Resumen comparativo de KPIs
        List<List<TipoColumna>> tiposPorFila = new ArrayList<>();
        for (KpiDTO k : datos.kpis()) {
            TipoColumna formatoValor = "numero".equals(k.formato()) ? TipoColumna.NUMERO : TipoColumna.MONEDA;
            tiposPorFila.add(List.of(TipoColumna.TEXTO, formatoValor, formatoValor, TipoColumna.PORCENTAJE, TipoColumna.TEXTO));
        }

        hojas.add(HojaExcelDTO.deTiposPorFila(
                "Resumen comparativo",
                NOMBRE,
                subtitulo,
                List.of("Indicador", "Periodo actual", "Periodo anterior", "Variacion %", "Tendencia"),
                tiposPorFila,
                datos.kpis().stream().map(k -> List.<Object>of(
                        k.label(),
                        k.actual(),
                        k.anterior(),
                        k.variacionPct() == null ? "Sin base" : k.variacionPct(),
                        etiquetaDireccion(k.direccion())
                )).toList()
        ));

        // 2. Comparativa por método
        hojas.add(hojaDesdeTabla(datos.porMetodo(), "Por método de pago", "Comparativa acumulada por método de pago",
                "Conciliación de importes teóricos vs reales declarados por cada flujo de cobro"));

        // 3. Distribución de resultados
        hojas.add(hojaDesdeTabla(datos.distribucion(), "Resultados de cuadre", "Distribución de resultados de corte",
                "Cantidad e impacto económico acumulado por resultado de auditoría"));

        // 4. Detalle cronológico
        hojas.add(hojaDesdeTabla(datos.detalle(), "Historial de cortes", "Registro cronológico de cortes diarios",
                "Trazabilidad inmutable de cortes de caja realizados en el período"));

        return excelService.generar(NOMBRE, hojas);
    }

    private AuditoriaCortesDiariosDTO construirReporte(Long tenantId, PeriodoComparativo periodo) {
        LocalDate desdeAct = periodo.actual().from().toLocalDate();
        LocalDate hastaAct = periodo.actual().to().toLocalDate();
        LocalDate desdeAnt = periodo.anterior().from().toLocalDate();
        LocalDate hastaAnt = periodo.anterior().to().toLocalDate();

        List<CorteCajaDiario> cortesAct = corteCajaDiarioRepository.findByTenantIdAndRangoFechas(tenantId, desdeAct, hastaAct);
        List<CorteCajaDiario> cortesAnt = corteCajaDiarioRepository.findByTenantIdAndRangoFechas(tenantId, desdeAnt, hastaAnt);

        // Agregaciones periodo actual
        long totalCortesAct = cortesAct.size();
        BigDecimal sisTotAct = cortesAct.stream().map(CorteCajaDiario::getSistemaTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal realTotAct = cortesAct.stream().map(CorteCajaDiario::getRealTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal diffTotAct = cortesAct.stream().map(CorteCajaDiario::getDiferenciaTotal).reduce(BigDecimal.ZERO, BigDecimal::add);

        long cuadradosAct = cortesAct.stream().filter(c -> "CUADRADO".equalsIgnoreCase(c.getEstadoDiferencia())).count();
        long sobrantesAct = cortesAct.stream().filter(c -> "SOBRANTE".equalsIgnoreCase(c.getEstadoDiferencia())).count();
        long faltantesAct = cortesAct.stream().filter(c -> "FALTANTE".equalsIgnoreCase(c.getEstadoDiferencia())).count();

        // Agregaciones periodo anterior
        long totalCortesAnt = cortesAnt.size();
        BigDecimal sisTotAnt = cortesAnt.stream().map(CorteCajaDiario::getSistemaTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal realTotAnt = cortesAnt.stream().map(CorteCajaDiario::getRealTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal diffTotAnt = cortesAnt.stream().map(CorteCajaDiario::getDiferenciaTotal).reduce(BigDecimal.ZERO, BigDecimal::add);

        // KPIs
        List<KpiDTO> kpis = new ArrayList<>();
        kpis.add(crearKpi("Cortes del día realizados", totalCortesAct, totalCortesAnt, "numero", false));
        kpis.add(crearKpi("Total Teórico (Sistema)", sisTotAct.doubleValue(), sisTotAnt.doubleValue(), "moneda", false));
        kpis.add(crearKpi("Total Físico (Declarado)", realTotAct.doubleValue(), realTotAnt.doubleValue(), "moneda", false));
        kpis.add(crearKpi("Diferencia Neta Acumulada", diffTotAct.doubleValue(), diffTotAnt.doubleValue(), "moneda", true));

        double efectividadCuadreAct = totalCortesAct > 0 ? ((double) cuadradosAct / totalCortesAct) * 100.0 : 0.0;
        double efectividadCuadreAnt = totalCortesAnt > 0 ? ((double) cortesAnt.stream().filter(c -> "CUADRADO".equalsIgnoreCase(c.getEstadoDiferencia())).count() / totalCortesAnt) * 100.0 : 0.0;
        kpis.add(crearKpi("Efectividad de Cuadre %", efectividadCuadreAct, efectividadCuadreAnt, "porcentaje", false));

        // Tabla por método de pago acumulada
        BigDecimal sisEf = cortesAct.stream().map(CorteCajaDiario::getSistemaEfectivo).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal realEf = cortesAct.stream().map(CorteCajaDiario::getRealEfectivo).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal diffEf = realEf.subtract(sisEf);

        BigDecimal sisTar = cortesAct.stream().map(CorteCajaDiario::getSistemaTarjeta).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal realTar = cortesAct.stream().map(CorteCajaDiario::getRealTarjeta).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal diffTar = realTar.subtract(sisTar);

        BigDecimal sisTrans = cortesAct.stream().map(CorteCajaDiario::getSistemaTransferencia).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal realTrans = cortesAct.stream().map(CorteCajaDiario::getRealTransferencia).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal diffTrans = realTrans.subtract(sisTrans);

        BigDecimal sisOtros = cortesAct.stream().map(CorteCajaDiario::getSistemaOtros).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal realOtros = cortesAct.stream().map(CorteCajaDiario::getRealOtros).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal diffOtros = realOtros.subtract(sisOtros);

        List<Map<String, Object>> filasMetodo = List.of(
                crearFilaMetodo("Efectivo Físico", sisEf, realEf, diffEf),
                crearFilaMetodo("Tarjeta / Terminal", sisTar, realTar, diffTar),
                crearFilaMetodo("Transferencia / SPEI", sisTrans, realTrans, diffTrans),
                crearFilaMetodo("Otros Métodos", sisOtros, realOtros, diffOtros),
                crearFilaMetodo("Total General Consolidado", sisTotAct, realTotAct, diffTotAct)
        );
        TablaReporteDTO tablaPorMetodo = TablaReporteDTO.de(COLUMNAS_METODO, filasMetodo);

        // Tabla de distribución
        BigDecimal impactoSobrantes = cortesAct.stream()
                .filter(c -> "SOBRANTE".equalsIgnoreCase(c.getEstadoDiferencia()))
                .map(CorteCajaDiario::getDiferenciaTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal impactoFaltantes = cortesAct.stream()
                .filter(c -> "FALTANTE".equalsIgnoreCase(c.getEstadoDiferencia()))
                .map(CorteCajaDiario::getDiferenciaTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Map<String, Object>> filasDist = new ArrayList<>();
        filasDist.add(crearFilaDistribucion("Cuadrados (Sin diferencia)", cuadradosAct, totalCortesAct, BigDecimal.ZERO));
        filasDist.add(crearFilaDistribucion("Con Sobrante (+)", sobrantesAct, totalCortesAct, impactoSobrantes));
        filasDist.add(crearFilaDistribucion("Con Faltante (-)", faltantesAct, totalCortesAct, impactoFaltantes));
        TablaReporteDTO tablaDistribucion = TablaReporteDTO.de(COLUMNAS_DISTRIBUCION, filasDist);

        // Tabla de detalle cronológico
        List<Map<String, Object>> filasDetalle = new ArrayList<>();
        for (CorteCajaDiario c : cortesAct) {
            Map<String, Object> valores = new LinkedHashMap<>();
            valores.put("folio", "#CD-" + String.format("%04d", c.getIdCorteDiario()));
            valores.put("fecha", c.getFechaCorte() != null ? c.getFechaCorte().format(FORMATO_FECHA_TABLA) : "");
            valores.put("hora", c.getFechaHoraRegistro() != null ? c.getFechaHoraRegistro().format(FORMATO_HORA_TABLA) : "");
            valores.put("cajero", c.getCajeroNombre() != null ? c.getCajeroNombre() : (c.getCajeroEmail() != null ? c.getCajeroEmail() : "Cajero"));
            valores.put("sistema", c.getSistemaTotal().doubleValue());
            valores.put("real", c.getRealTotal().doubleValue());
            valores.put("diferencia", c.getDiferenciaTotal().doubleValue());
            valores.put("estado", c.getEstadoDiferencia());
            valores.put("comentarios", c.getComentarios() != null && !c.getComentarios().isBlank() ? c.getComentarios() : "Sin observaciones registradas");

            filasDetalle.add(valores);
        }
        TablaReporteDTO tablaDetalle = TablaReporteDTO.de(COLUMNAS_DETALLE, filasDetalle);

        List<CorteCajaDiarioDTO> listaCortes = cortesAct.stream().map(this::mapToDTO).toList();

        ReporteMetaDTO meta = ReporteMetaDTO.de(
                REPORTE_KEY,
                PILAR,
                NOMBRE,
                periodo,
                "day"
        );

        return AuditoriaCortesDiariosDTO.de(meta, kpis, tablaPorMetodo, tablaDistribucion, tablaDetalle, listaCortes);
    }

    private Map<String, Object> crearFilaMetodo(String nombreMetodo, BigDecimal sis, BigDecimal real, BigDecimal diff) {
        Map<String, Object> val = new LinkedHashMap<>();
        val.put("metodo", nombreMetodo);
        val.put("sistema", sis.doubleValue());
        val.put("real", real.doubleValue());
        val.put("diferencia", diff.doubleValue());

        String estado;
        if (diff.compareTo(BigDecimal.ZERO) == 0) {
            estado = "CUADRADO";
        } else if (diff.compareTo(BigDecimal.ZERO) > 0) {
            estado = "SOBRANTE";
        } else {
            estado = "FALTANTE";
        }
        val.put("estado", estado);

        return val;
    }

    private Map<String, Object> crearFilaDistribucion(String estado, long cant, long totalCortes, BigDecimal montoDiff) {
        Map<String, Object> val = new LinkedHashMap<>();
        val.put("estado", estado);
        val.put("cantidad", cant);
        double pct = totalCortes > 0 ? ((double) cant / totalCortes) * 100.0 : 0.0;
        val.put("pct", BigDecimal.valueOf(pct).setScale(1, RoundingMode.HALF_UP).doubleValue());
        val.put("montoDiferencia", montoDiff.doubleValue());
        return val;
    }

    private ResumenCorteSistemaDTO calcularResumenSistemaHoy(Long tenantId, LocalDate hoy) {
        LocalDateTime desde = hoy.atStartOfDay();
        LocalDateTime hasta = hoy.plusDays(1).atStartOfDay();

        List<Pago> pagos = pagoRepository.findPagosEnRango(tenantId, null, desde, hasta);

        BigDecimal sisEf = BigDecimal.ZERO;
        BigDecimal sisTar = BigDecimal.ZERO;
        BigDecimal sisTrans = BigDecimal.ZERO;
        BigDecimal sisOtros = BigDecimal.ZERO;
        BigDecimal totProp = BigDecimal.ZERO;

        Set<UUID> comandasIds = new HashSet<>();
        long articulosVendidos = 0L;

        for (Pago p : pagos) {
            BigDecimal cuenta = p.getMontoCuenta() != null ? p.getMontoCuenta() : BigDecimal.ZERO;
            BigDecimal propina = p.getMontoPropina() != null ? p.getMontoPropina() : BigDecimal.ZERO;
            totProp = totProp.add(propina);
            BigDecimal totalCobrado = cuenta.add(propina);

            String metRaw = p.getMetodoPago() != null ? p.getMetodoPago().trim().toUpperCase() : "EFECTIVO";
            switch (metRaw) {
                case "CASH", "EFECTIVO", "DINERO" -> sisEf = sisEf.add(totalCobrado);
                case "CARD", "TARJETA", "VISA", "MASTERCARD", "DEBITO", "CREDITO" -> sisTar = sisTar.add(totalCobrado);
                case "TRANSFER", "TRANSFERENCIA", "SPEI" -> sisTrans = sisTrans.add(totalCobrado);
                default -> sisOtros = sisOtros.add(totalCobrado);
            }

            if (p.getComanda() != null) {
                comandasIds.add(p.getComanda().getId());
                if (p.getComanda().getItems() != null) {
                    articulosVendidos += p.getComanda().getItems().stream()
                            .mapToLong(i -> i.getCantidad() != null ? i.getCantidad() : 1)
                            .sum();
                }
            }
        }

        BigDecimal sisTotal = sisEf.add(sisTar).add(sisTrans).add(sisOtros);

        return ResumenCorteSistemaDTO.builder()
                .fecha(hoy)
                .sistemaEfectivo(sisEf)
                .sistemaTarjeta(sisTar)
                .sistemaTransferencia(sisTrans)
                .sistemaOtros(sisOtros)
                .sistemaTotal(sisTotal)
                .totalComandas((long) comandasIds.size())
                .totalArticulos(articulosVendidos)
                .totalPropinas(totProp)
                .build();
    }

    private CorteCajaDiarioDTO mapToDTO(CorteCajaDiario c) {
        return CorteCajaDiarioDTO.builder()
                .idCorteDiario(c.getIdCorteDiario())
                .tenantId(c.getTenant().getId())
                .cajeroId(c.getCajero() != null ? c.getCajero().getId() : null)
                .cajeroNombre(c.getCajeroNombre())
                .cajeroEmail(c.getCajeroEmail())
                .fechaCorte(c.getFechaCorte())
                .fechaHoraRegistro(c.getFechaHoraRegistro())
                .sistemaEfectivo(c.getSistemaEfectivo())
                .sistemaTarjeta(c.getSistemaTarjeta())
                .sistemaTransferencia(c.getSistemaTransferencia())
                .sistemaOtros(c.getSistemaOtros())
                .sistemaTotal(c.getSistemaTotal())
                .realEfectivo(c.getRealEfectivo())
                .realTarjeta(c.getRealTarjeta())
                .realTransferencia(c.getRealTransferencia())
                .realOtros(c.getRealOtros())
                .realTotal(c.getRealTotal())
                .diferenciaEfectivo(c.getDiferenciaEfectivo())
                .diferenciaTarjeta(c.getDiferenciaTarjeta())
                .diferenciaTransferencia(c.getDiferenciaTransferencia())
                .diferenciaOtros(c.getDiferenciaOtros())
                .diferenciaTotal(c.getDiferenciaTotal())
                .estadoDiferencia(c.getEstadoDiferencia())
                .comentarios(c.getComentarios())
                .totalComandas(c.getTotalComandas())
                .totalArticulos(c.getTotalArticulos())
                .totalPropinas(c.getTotalPropinas())
                .build();
    }

    private KpiDTO crearKpi(String label, double actual, double anterior, String formato, boolean invertirTendencia) {
        String key = label.toLowerCase().replace(" ", "_");
        BigDecimal actualSafe = BigDecimal.valueOf(actual).setScale(2, RoundingMode.HALF_UP);
        BigDecimal anteriorSafe = BigDecimal.valueOf(anterior).setScale(2, RoundingMode.HALF_UP);
        KpiDTO kpi = KpiDTO.de(key, label, actualSafe, anteriorSafe, formato);
        if (invertirTendencia && kpi.variacionPct() != null) {
            String dir = kpi.direccion();
            if (KpiDTO.SUBE.equals(dir)) dir = KpiDTO.BAJA;
            else if (KpiDTO.BAJA.equals(dir)) dir = KpiDTO.SUBE;
            return new KpiDTO(key, label, actualSafe, anteriorSafe, kpi.variacionPct(), dir, formato);
        }
        return kpi;
    }

    private String etiquetaDireccion(String direccion) {
        if (KpiDTO.SUBE.equalsIgnoreCase(direccion)) return "Crecimiento (+)";
        if (KpiDTO.BAJA.equalsIgnoreCase(direccion)) return "Disminucion (-)";
        if (KpiDTO.NUEVO.equalsIgnoreCase(direccion)) return "Nuevo (sin base)";
        return "Estable (=)";
    }

    private HojaExcelDTO hojaDesdeTabla(TablaReporteDTO tabla, String tituloHoja, String titulo, String descripcion) {
        List<String> encabezados = tabla.columnas().stream().map(ColumnaDTO::label).toList();
        List<TipoColumna> tipos = tabla.columnas().stream().map(ColumnaDTO::tipo).toList();
        List<String> claves = tabla.columnas().stream().map(ColumnaDTO::key).toList();

        return HojaExcelDTO.de(
                tituloHoja,
                titulo,
                descripcion,
                encabezados,
                tipos,
                excelService.aFilas(claves, tabla.filas())
        );
    }
}
