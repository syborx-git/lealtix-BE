package com.lealtixservice.controller;

import com.lealtixservice.dto.GenericResponse;
import com.lealtixservice.dto.reportes.AuditoriaMermasDTO;
import com.lealtixservice.dto.reportes.CorteCajaDTO;
import com.lealtixservice.dto.reportes.PresetReporte;
import com.lealtixservice.dto.reportes.StockMinimoReporteDTO;
import com.lealtixservice.dto.reportes.VentasTendenciasDTO;
import com.lealtixservice.service.ReporteCorteCajaService;
import com.lealtixservice.dto.reportes.AuditoriaTicketsCanceladosDTO;
import com.lealtixservice.service.ReporteExcelService;
import com.lealtixservice.service.ReporteMermasService;
import com.lealtixservice.service.ReporteStockMinimoService;
import com.lealtixservice.service.ReporteTicketsCanceladosService;
import com.lealtixservice.service.ReporteVentasService;
import com.lealtixservice.util.RequirePermission;
import com.lealtixservice.util.TenantOwnership;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Modulo maestro de Reportes y Analitica.
 *
 * Todos los reportes comparten el mismo contrato: filtros rapidos por preset,
 * rango personalizado y comparativa automatica contra el periodo anterior
 * equivalente (resuelto en el backend, no en la UI). La exportacion a Excel la
 * resuelve ReporteExcelService, de modo que anadir un reporte nuevo no obliga a
 * tocar el generador de archivos.
 */
@Slf4j
@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
@Tag(name = "Reportes", description = "Modulo de reportes y analitica con comparativas periodicas y exportacion a Excel")
public class ReportesController {

    private static final String XLSX_MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final ReporteVentasService reporteVentasService;
    private final ReporteCorteCajaService reporteCorteCajaService;
    private final ReporteMermasService reporteMermasService;
    private final ReporteStockMinimoService reporteStockMinimoService;
    private final ReporteTicketsCanceladosService reporteTicketsCanceladosService;
    private final ReporteExcelService excelService;

    // ==================== 1.1 Dashboard de Ventas y Tendencias ====================

    @Operation(summary = "Reporte 1.1: Dashboard de Ventas y Tendencias",
            description = "KPIs de ingresos brutos/netos, descuentos, ticket promedio y clientes unicos, "
                    + "con serie temporal y comparativa automatica contra el periodo anterior equivalente. "
                    + "Enviar preset (HOY, AYER, ESTA_SEMANA, SEMANA_PASADA, ESTE_MES, MES_PASADO, PERSONALIZADO). "
                    + "Para PERSONALIZADO se deben enviar from y to.")
    @GetMapping("/ventas")
    @RequirePermission(value = "view_reports", alternative = {"view_sales", "manage_all"})
    @TenantOwnership(tenantIdParam = "tenantId")
    public ResponseEntity<?> getVentasTendencias(
            @Parameter(description = "ID del tenant") @RequestParam Long tenantId,
            @Parameter(description = "Filtro rapido: HOY, AYER, ESTA_SEMANA, SEMANA_PASADA, ESTE_MES, MES_PASADO, PERSONALIZADO")
            @RequestParam(required = false) String preset,
            @Parameter(description = "Fecha inicio (solo para PERSONALIZADO)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "Fecha fin (solo para PERSONALIZADO)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @Parameter(description = "Granularidad de la serie: day, week o month (si se omite se elige automaticamente)")
            @RequestParam(required = false) String granularidad) {

        PresetReporte p = PresetReporte.from(preset);
        log.info("GET /api/reportes/ventas - tenantId={}, preset={}, from={}, to={}, granularidad={}",
                tenantId, p, from, to, granularidad);

        try {
            VentasTendenciasDTO reporte = reporteVentasService.obtener(tenantId, p, from, to, granularidad);
            return ResponseEntity.ok(new GenericResponse(200, "Reporte de ventas generado", reporte));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new GenericResponse(400, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error generando el reporte de ventas", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse(500, "Error interno generando el reporte de ventas", null));
        }
    }

    @Operation(summary = "Reporte 1.1: exportar Dashboard de Ventas a Excel",
            description = "Genera un .xlsx real con hojas de resumen comparativo, tendencia, ventas por categoria "
                    + "y top productos. Respeta exactamente el mismo rango y comparativa que el reporte en pantalla.")
    @GetMapping("/ventas/export")
    @RequirePermission(value = "view_reports", alternative = {"view_sales", "manage_all"})
    @TenantOwnership(tenantIdParam = "tenantId")
    public ResponseEntity<?> exportarVentasTendencias(
            @Parameter(description = "ID del tenant") @RequestParam Long tenantId,
            @Parameter(description = "Filtro rapido: HOY, AYER, ESTA_SEMANA, SEMANA_PASADA, ESTE_MES, MES_PASADO, PERSONALIZADO")
            @RequestParam(required = false) String preset,
            @Parameter(description = "Fecha inicio (solo para PERSONALIZADO)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "Fecha fin (solo para PERSONALIZADO)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @Parameter(description = "Granularidad de la serie: day, week o month")
            @RequestParam(required = false) String granularidad) {

        PresetReporte p = PresetReporte.from(preset);
        log.info("GET /api/reportes/ventas/export - tenantId={}, preset={}, from={}, to={}",
                tenantId, p, from, to);

        try {
            byte[] archivo = reporteVentasService.exportar(tenantId, p, from, to, granularidad);
            String nombre = excelService.nombreArchivo(
                    "ventas_" + p.name().toLowerCase() + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")));

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                    .contentType(MediaType.parseMediaType(XLSX_MIME))
                    .contentLength(archivo.length)
                    .body(archivo);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new GenericResponse(400, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error inesperado exportando el reporte de ventas", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse(500, "Error interno exportando el reporte", null));
        }
    }

    // ==================== 1.2 Corte de Caja y Conciliacion ====================

    @Operation(summary = "Reporte 1.2: Corte de Caja y Conciliacion",
            description = "Total cobrado con desglose por metodo de pago (efectivo, tarjeta, transferencia y mixto), "
                    + "conciliacion por cajero y anulaciones del periodo, con comparativa automatica contra el "
                    + "periodo anterior equivalente. El cobro se obtiene de comanda_pago (division de cuenta) "
                    + "combinado con los pagos simples de client_order, sin duplicar importes.")
    @GetMapping("/corte-caja")
    @RequirePermission(value = "view_reports", alternative = {"view_sales", "manage_all"})
    @TenantOwnership(tenantIdParam = "tenantId")
    public ResponseEntity<?> getCorteCaja(
            @Parameter(description = "ID del tenant") @RequestParam Long tenantId,
            @Parameter(description = "Filtro rapido: HOY, AYER, ESTA_SEMANA, SEMANA_PASADA, ESTE_MES, MES_PASADO, PERSONALIZADO")
            @RequestParam(required = false) String preset,
            @Parameter(description = "Fecha inicio (solo para PERSONALIZADO)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "Fecha fin (solo para PERSONALIZADO)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        PresetReporte p = PresetReporte.from(preset);
        log.info("GET /api/reportes/corte-caja - tenantId={}, preset={}, from={}, to={}", tenantId, p, from, to);

        try {
            CorteCajaDTO reporte = reporteCorteCajaService.obtener(tenantId, p, from, to);
            return ResponseEntity.ok(new GenericResponse(200, "Reporte de corte de caja generado", reporte));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new GenericResponse(400, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error generando el reporte de corte de caja", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse(500, "Error interno generando el reporte de corte de caja", null));
        }
    }

    @Operation(summary = "Reporte 1.2: exportar Corte de Caja a Excel",
            description = "Genera un .xlsx con hojas de resumen comparativo, desglose por metodo de pago, "
                    + "conciliacion por cajero y anulaciones del periodo.")
    @GetMapping("/corte-caja/export")
    @RequirePermission(value = "view_reports", alternative = {"view_sales", "manage_all"})
    @TenantOwnership(tenantIdParam = "tenantId")
    public ResponseEntity<?> exportarCorteCaja(
            @Parameter(description = "ID del tenant") @RequestParam Long tenantId,
            @Parameter(description = "Filtro rapido: HOY, AYER, ESTA_SEMANA, SEMANA_PASADA, ESTE_MES, MES_PASADO, PERSONALIZADO")
            @RequestParam(required = false) String preset,
            @Parameter(description = "Fecha inicio (solo para PERSONALIZADO)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "Fecha fin (solo para PERSONALIZADO)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        PresetReporte p = PresetReporte.from(preset);
        log.info("GET /api/reportes/corte-caja/export - tenantId={}, preset={}, from={}, to={}", tenantId, p, from, to);

        try {
            byte[] archivo = reporteCorteCajaService.exportar(tenantId, p, from, to);
            String nombre = excelService.nombreArchivo(
                    "corte_caja_" + p.name().toLowerCase() + "_"
                            + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")));

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                    .contentType(MediaType.parseMediaType(XLSX_MIME))
                    .contentLength(archivo.length)
                    .body(archivo);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new GenericResponse(400, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error inesperado exportando el reporte de corte de caja", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse(500, "Error interno exportando el reporte", null));
        }
    }

    // ==================== 2.3 Auditoria de Mermas ====================

    @Operation(summary = "Reporte 2.3: Auditoria de Mermas",
            description = "Costeo de salidas no-venta por motivo, responsable e insumos, "
                    + "con comparativa automatica contra el periodo anterior equivalente.")
    @GetMapping("/mermas")
    @RequirePermission(value = "view_reports", alternative = {"manage_mermas", "view_products", "manage_all"})
    @TenantOwnership(tenantIdParam = "tenantId")
    public ResponseEntity<?> getAuditoriaMermas(
            @Parameter(description = "ID del tenant") @RequestParam Long tenantId,
            @Parameter(description = "Filtro rapido: HOY, AYER, ESTA_SEMANA, SEMANA_PASADA, ESTE_MES, MES_PASADO, PERSONALIZADO")
            @RequestParam(required = false) String preset,
            @Parameter(description = "Fecha inicio (solo para PERSONALIZADO)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "Fecha fin (solo para PERSONALIZADO)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        PresetReporte p = PresetReporte.from(preset);
        log.info("GET /api/reportes/mermas - tenantId={}, preset={}, from={}, to={}", tenantId, p, from, to);

        try {
            AuditoriaMermasDTO reporte = reporteMermasService.obtener(tenantId, p, from, to);
            return ResponseEntity.ok(new GenericResponse(200, "Reporte de auditoria de mermas generado", reporte));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new GenericResponse(400, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error generando el reporte de auditoria de mermas", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse(500, "Error interno generando el reporte de mermas", null));
        }
    }

    @Operation(summary = "Reporte 2.3: exportar Auditoria de Mermas a Excel",
            description = "Genera un .xlsx con hojas de resumen comparativo, desgloses por motivo, "
                    + "responsable, top insumos y detalle cronologico.")
    @GetMapping("/mermas/export")
    @RequirePermission(value = "view_reports", alternative = {"manage_mermas", "view_products", "manage_all"})
    @TenantOwnership(tenantIdParam = "tenantId")
    public ResponseEntity<?> exportarAuditoriaMermas(
            @Parameter(description = "ID del tenant") @RequestParam Long tenantId,
            @Parameter(description = "Filtro rapido: HOY, AYER, ESTA_SEMANA, SEMANA_PASADA, ESTE_MES, MES_PASADO, PERSONALIZADO")
            @RequestParam(required = false) String preset,
            @Parameter(description = "Fecha inicio (solo para PERSONALIZADO)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "Fecha fin (solo para PERSONALIZADO)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        PresetReporte p = PresetReporte.from(preset);
        log.info("GET /api/reportes/mermas/export - tenantId={}, preset={}, from={}, to={}", tenantId, p, from, to);

        try {
            byte[] archivo = reporteMermasService.exportar(tenantId, p, from, to);
            String nombre = excelService.nombreArchivo(
                    "mermas_" + p.name().toLowerCase() + "_"
                            + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")));

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                    .contentType(MediaType.parseMediaType(XLSX_MIME))
                    .contentLength(archivo.length)
                    .body(archivo);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new GenericResponse(400, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error inesperado exportando el reporte de mermas", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse(500, "Error interno exportando el reporte de mermas", null));
        }
    }

    // ==================== 2.4 Alertas de Stock Minimo y Critico ====================

    @Operation(summary = "Reporte 2.4: Alertas de Stock Minimo y Critico",
            description = "Auditoria de insumos y productos con stock critico o agotado, "
                    + "calculo de cantidades a reabastecer e inversion estimada de compra.")
    @GetMapping("/stock-minimo")
    @RequirePermission(value = "view_reports", alternative = {"view_products", "manage_all"})
    @TenantOwnership(tenantIdParam = "tenantId")
    public ResponseEntity<?> getAlertasStockMinimo(
            @Parameter(description = "ID del tenant") @RequestParam Long tenantId) {

        log.info("GET /api/reportes/stock-minimo - tenantId={}", tenantId);

        try {
            StockMinimoReporteDTO reporte = reporteStockMinimoService.obtener(tenantId);
            return ResponseEntity.ok(new GenericResponse(200, "Reporte de alertas de stock generado", reporte));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new GenericResponse(400, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error generando el reporte de alertas de stock minimo", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse(500, "Error interno generando el reporte de stock", null));
        }
    }

    @Operation(summary = "Reporte 2.4: exportar Alertas de Stock Minimo a Excel",
            description = "Genera un .xlsx con hojas de resumen de stock, lista de compras recomendada "
                    + "y auditoria de inventario general.")
    @GetMapping("/stock-minimo/export")
    @RequirePermission(value = "view_reports", alternative = {"view_products", "manage_all"})
    @TenantOwnership(tenantIdParam = "tenantId")
    public ResponseEntity<?> exportarAlertasStockMinimo(
            @Parameter(description = "ID del tenant") @RequestParam Long tenantId) {

        log.info("GET /api/reportes/stock-minimo/export - tenantId={}", tenantId);

        try {
            byte[] archivo = reporteStockMinimoService.exportar(tenantId);
            String nombre = excelService.nombreArchivo(
                    "stock_minimo_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")));

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                    .contentType(MediaType.parseMediaType(XLSX_MIME))
                    .contentLength(archivo.length)
                    .body(archivo);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new GenericResponse(400, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error inesperado exportando el reporte de stock", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse(500, "Error interno exportando el reporte de stock", null));
        }
    }

    // ==================== 4.1 Auditoría de Tickets Cancelados ====================

    @Operation(summary = "Reporte 4.1: Auditoría de Tickets Cancelados",
            description = "KPIs de comandas anuladas, monto económico no percibido, tasa de cancelación, "
                    + "distribución por motivo, trazabilidad por responsable y detalle cronológico.")
    @GetMapping("/tickets-cancelados")
    @RequirePermission(value = "view_reports", alternative = {"view_sales", "manage_all"})
    @TenantOwnership(tenantIdParam = "tenantId")
    public ResponseEntity<?> getAuditoriaTicketsCancelados(
            @Parameter(description = "ID del tenant") @RequestParam Long tenantId,
            @Parameter(description = "Filtro rápido: HOY, AYER, ESTA_SEMANA, SEMANA_PASADA, ESTE_MES, MES_PASADO, PERSONALIZADO")
            @RequestParam(required = false) String preset,
            @Parameter(description = "Fecha inicio (solo para PERSONALIZADO)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "Fecha fin (solo para PERSONALIZADO)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        PresetReporte p = PresetReporte.from(preset);
        log.info("GET /api/reportes/tickets-cancelados - tenantId={}, preset={}, from={}, to={}",
                tenantId, p, from, to);

        try {
            AuditoriaTicketsCanceladosDTO reporte = reporteTicketsCanceladosService.obtener(tenantId, p, from, to);
            return ResponseEntity.ok(new GenericResponse(200, "Reporte de tickets cancelados generado", reporte));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new GenericResponse(400, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error inesperado generando la auditoría de tickets cancelados", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse(500, "Error interno generando la auditoría de tickets cancelados", null));
        }
    }

    @Operation(summary = "Reporte 4.1: Exportar Auditoría de Tickets Cancelados a Excel",
            description = "Genera un archivo .xlsx con hojas de resumen de KPIs, desglose por motivo, "
                    + "desglose por responsable y detalle cronológico de cancelaciones.")
    @GetMapping("/tickets-cancelados/export")
    @RequirePermission(value = "view_reports", alternative = {"view_sales", "manage_all"})
    @TenantOwnership(tenantIdParam = "tenantId")
    public ResponseEntity<?> exportarAuditoriaTicketsCancelados(
            @Parameter(description = "ID del tenant") @RequestParam Long tenantId,
            @Parameter(description = "Filtro rápido: HOY, AYER, ESTA_SEMANA, SEMANA_PASADA, ESTE_MES, MES_PASADO, PERSONALIZADO")
            @RequestParam(required = false) String preset,
            @Parameter(description = "Fecha inicio (solo para PERSONALIZADO)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "Fecha fin (solo para PERSONALIZADO)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        PresetReporte p = PresetReporte.from(preset);
        log.info("GET /api/reportes/tickets-cancelados/export - tenantId={}, preset={}, from={}, to={}",
                tenantId, p, from, to);

        try {
            byte[] archivo = reporteTicketsCanceladosService.exportar(tenantId, p, from, to);
            String nombre = excelService.nombreArchivo(
                    "tickets_cancelados_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")));

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                    .contentType(MediaType.parseMediaType(XLSX_MIME))
                    .contentLength(archivo.length)
                    .body(archivo);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new GenericResponse(400, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error inesperado exportando el reporte de tickets cancelados", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse(500, "Error interno exportando el reporte de tickets cancelados", null));
        }
    }
}
