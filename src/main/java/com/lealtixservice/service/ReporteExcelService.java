package com.lealtixservice.service;

import com.lealtixservice.dto.reportes.HojaExcelDTO;
import com.lealtixservice.dto.reportes.TipoColumna;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio centralizado de exportacion a Excel del modulo de reportes.
 *
 * No conoce ningun reporte concreto: recibe una lista de hojas genéricas y produce
 * un .xlsx real. Todos los reportes de los 4 pilares pasan por aqui, de modo que
 * el formato (cabeceras en negrita, auto-ajuste de columnas, formatos numericos y
 * de moneda, filtros y paneles congelados) es identico en las 12 exportaciones.
 */
@Slf4j
@Service
public class ReporteExcelService {

    private static final String FORMATO_MONEDA = "\"$\"#,##0.00";
    private static final String FORMATO_NUMERO = "#,##0.00";
    private static final String FORMATO_ENTERO = "#,##0";
    private static final String FORMATO_PORCENTAJE = "0.00";
    private static final String FORMATO_FECHA = "dd/MM/yyyy";
    private static final String FORMATO_FECHA_HORA = "dd/MM/yyyy HH:mm";

    private static final int ANCHO_MAXIMO_CELDA = 60 * 256;
    private static final int MAX_CARACTERES_NOMBRE_HOJA = 31;

    /**
     * Genera el archivo .xlsx a partir de las hojas receivable.
     *
     * @param nombreArchivoBase nombre base sin extension (el timestamp lo anade el controller)
     * @param hojas             hojas a incluir, en orden
     * @return bytes del archivo .xlsx
     */
    public byte[] generar(String nombreArchivoBase, List<HojaExcelDTO> hojas) {
        if (hojas == null || hojas.isEmpty()) {
            throw new IllegalArgumentException("El reporte no contiene hojas para exportar");
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Map<String, CellStyle> estilos = new HashMap<>();
            CellStyle estiloTitulo = crearEstiloTitulo(workbook);
            CellStyle estiloSubtitulo = crearEstiloSubtitulo(workbook);
            CellStyle estiloEncabezado = crearEstiloEncabezado(workbook);

            for (HojaExcelDTO hoja : hojas) {
                escribirHoja(workbook, hoja, estilos, estiloTitulo, estiloSubtitulo, estiloEncabezado);
            }

            workbook.write(out);
            log.debug("Excel generado: {} con {} hoja(s)", nombreArchivoBase, hojas.size());
            return out.toByteArray();
        } catch (IOException e) {
            log.error("Error al generar el Excel del reporte {}", nombreArchivoBase, e);
            throw new IllegalStateException("No se pudo generar el archivo Excel", e);
        }
    }

    private void escribirHoja(XSSFWorkbook workbook, HojaExcelDTO hoja,
                              Map<String, CellStyle> estilos,
                              CellStyle estiloTitulo, CellStyle estiloSubtitulo, CellStyle estiloEncabezado) {

        Sheet sheet = workbook.createSheet(normalizarNombreHoja(hoja.nombre(), workbook.getNumberOfSheets()));
        int totalColumnas = Math.max(hoja.encabezados() == null ? 0 : hoja.encabezados().size(), 1);

        int filaActual = 0;

        if (hoja.titulo() != null && !hoja.titulo().isBlank()) {
            Row filaTitulo = sheet.createRow(filaActual++);
            Cell celda = filaTitulo.createCell(0);
            celda.setCellValue(hoja.titulo());
            celda.setCellStyle(estiloTitulo);
            fusionar(sheet, filaActual - 1, totalColumnas);
            filaTitulo.setHeightInPoints(20f);
        }

        if (hoja.subtitulo() != null && !hoja.subtitulo().isBlank()) {
            Row filaSubtitulo = sheet.createRow(filaActual++);
            Cell celda = filaSubtitulo.createCell(0);
            celda.setCellValue(hoja.subtitulo());
            celda.setCellStyle(estiloSubtitulo);
            fusionar(sheet, filaActual - 1, totalColumnas);
        }

        if (filaActual > 0) {
            filaActual++; // linea en blanco entre el encabezado y la tabla
        }

        int filaEncabezado = filaActual;
        List<String> encabezados = hoja.encabezados() == null ? List.of() : hoja.encabezados();
        if (!encabezados.isEmpty()) {
            Row row = sheet.createRow(filaEncabezado);
            for (int i = 0; i < encabezados.size(); i++) {
                Cell celda = row.createCell(i);
                celda.setCellValue(encabezados.get(i));
                celda.setCellStyle(estiloEncabezado);
            }
            filaActual++;
        }

        List<TipoColumna> tipos = hoja.tipos() == null ? List.of() : hoja.tipos();
        List<List<TipoColumna>> tiposPorFila = hoja.tiposPorFila() == null ? List.of() : hoja.tiposPorFila();
        List<List<Object>> filas = hoja.filas() == null ? List.of() : hoja.filas();

        for (int indiceFila = 0; indiceFila < filas.size(); indiceFila++) {
            List<Object> fila = filas.get(indiceFila);
            // La hoja puede declarar un tipo por celda (formatos mixtos en la misma
            // columna); si no, aplica el mismo tipo a toda la fila.
            List<TipoColumna> tiposFila = (indiceFila < tiposPorFila.size() && !tiposPorFila.get(indiceFila).isEmpty())
                    ? tiposPorFila.get(indiceFila)
                    : tipos;
            Row row = sheet.createRow(filaActual);
            for (int col = 0; col < fila.size(); col++) {
                TipoColumna tipo = col < tiposFila.size() ? tiposFila.get(col) : TipoColumna.TEXTO;
                Cell celda = row.createCell(col);
                escribirValor(celda, fila.get(col), tipo);
                celda.setCellStyle(estiloCelda(workbook, estilos, tipo));
            }
            filaActual++;
        }

        if (!encabezados.isEmpty() && filaActual > filaEncabezado + 1) {
            sheet.setAutoFilter(new CellRangeAddress(
                    filaEncabezado, filaActual - 1, 0, encabezados.size() - 1));
        }

        // Panel congelado: el titulo y la cabecera siguen visibles al desplazar.
        sheet.createFreezePane(0, filaEncabezado + 1);

        autoAjustarColumnas(sheet, totalColumnas);
    }

    private void escribirValor(Cell celda, Object valor, TipoColumna tipo) {
        if (valor == null) {
            return;
        }
        switch (tipo) {
            case MONEDA, NUMERO, PORCENTAJE -> {
                BigDecimal numero = aDecimal(valor);
                if (numero != null) {
                    celda.setCellValue(numero.doubleValue());
                    return;
                }
                celda.setCellValue(valor.toString());
            }
            case FECHA -> {
                if (valor instanceof LocalDate fecha) {
                    celda.setCellValue(fecha);
                } else if (valor instanceof LocalDateTime fechaHora) {
                    celda.setCellValue(fechaHora);
                } else {
                    celda.setCellValue(valor.toString());
                }
            }
            default -> celda.setCellValue(valor.toString());
        }
    }

    private BigDecimal aDecimal(Object valor) {
        if (valor instanceof BigDecimal decimal) {
            return decimal;
        }
        if (valor instanceof Number numero) {
            return BigDecimal.valueOf(numero.doubleValue());
        }
        try {
            return new BigDecimal(valor.toString().replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void autoAjustarColumnas(Sheet sheet, int totalColumnas) {
        for (int i = 0; i < totalColumnas; i++) {
            try {
                sheet.autoSizeColumn(i);
                int ancho = sheet.getColumnWidth(i);
                int conMargen = ancho + 1024;
                if (conMargen > ANCHO_MAXIMO_CELDA) {
                    sheet.setColumnWidth(i, ANCHO_MAXIMO_CELDA);
                }
            } catch (Exception e) {
                // autoSizeColumn depende de la fuente del sistema: si falla, se deja el ancho por defecto.
                log.debug("No se pudo auto-ajustar la columna {} de la hoja {}", i, sheet.getSheetName());
            }
        }
    }

    private void fusionar(Sheet sheet, int fila, int totalColumnas) {
        if (totalColumnas > 1) {
            sheet.addMergedRegion(new CellRangeAddress(fila, fila, 0, totalColumnas - 1));
        }
    }

    private CellStyle estiloCelda(XSSFWorkbook workbook, Map<String, CellStyle> estilos, TipoColumna tipo) {
        String clave = "celda_" + tipo.name();
        return estilos.computeIfAbsent(clave, k -> {
            CellStyle estilo = workbook.createCellStyle();
            estilo.setBorderBottom(BorderStyle.HAIR);
            estilo.setBorderTop(BorderStyle.HAIR);
            estilo.setBorderLeft(BorderStyle.HAIR);
            estilo.setBorderRight(BorderStyle.HAIR);
            estilo.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
            estilo.setDataFormat(workbook.createDataFormat().getFormat(formatoDe(tipo)));
            if (tipo != TipoColumna.TEXTO) {
                estilo.setAlignment(HorizontalAlignment.RIGHT);
            }
            return estilo;
        });
    }

    private String formatoDe(TipoColumna tipo) {
        return switch (tipo) {
            case MONEDA -> FORMATO_MONEDA;
            case NUMERO -> FORMATO_NUMERO;
            case PORCENTAJE -> FORMATO_PORCENTAJE;
            case FECHA -> FORMATO_FECHA;
            default -> "General";
        };
    }

    private CellStyle crearEstiloTitulo(XSSFWorkbook workbook) {
        Font fuente = workbook.createFont();
        fuente.setBold(true);
        fuente.setFontHeightInPoints((short) 13);
        CellStyle estilo = workbook.createCellStyle();
        estilo.setFont(fuente);
        estilo.setAlignment(HorizontalAlignment.LEFT);
        return estilo;
    }

    private CellStyle crearEstiloSubtitulo(XSSFWorkbook workbook) {
        Font fuente = workbook.createFont();
        fuente.setFontHeightInPoints((short) 10);
        fuente.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        CellStyle estilo = workbook.createCellStyle();
        estilo.setFont(fuente);
        estilo.setAlignment(HorizontalAlignment.LEFT);
        return estilo;
    }

    private CellStyle crearEstiloEncabezado(XSSFWorkbook workbook) {
        Font fuente = workbook.createFont();
        fuente.setBold(true);
        fuente.setColor(IndexedColors.WHITE.getIndex());

        CellStyle estilo = workbook.createCellStyle();
        estilo.setFont(fuente);
        estilo.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        estilo.setAlignment(HorizontalAlignment.CENTER);
        estilo.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
        estilo.setWrapText(true);
        estilo.setBorderBottom(BorderStyle.THIN);
        return estilo;
    }

    /** Excel restringe el nombre de hoja a 31 caracteres y sin caracteres especiales. */
    private String normalizarNombreHoja(String nombre, int indice) {
        if (nombre == null || nombre.isBlank()) {
            return "Hoja " + (indice + 1);
        }
        String limpio = nombre.replaceAll("[\\[\\]:*?/\\\\]", " ").trim();
        if (limpio.isEmpty()) {
            return "Hoja " + (indice + 1);
        }
        if (limpio.length() > MAX_CARACTERES_NOMBRE_HOJA) {
            return limpio.substring(0, MAX_CARACTERES_NOMBRE_HOJA);
        }
        return limpio;
    }

    /** Nombre de archivo seguro para el header Content-Disposition. */
    public String nombreArchivo(String nombreBase) {
        String limpio = nombreBase == null ? "reporte" : nombreBase.trim();
        limpio = limpio.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (limpio.isEmpty()) {
            limpio = "reporte";
        }
        return limpio.endsWith(".xlsx") ? limpio : limpio + ".xlsx";
    }

    /** Convierte una lista de mapas en filas tabulares segun el orden de las columnas. */
    public List<List<Object>> aFilas(List<String> claves, List<Map<String, Object>> datos) {
        List<List<Object>> filas = new ArrayList<>();
        if (datos == null) {
            return filas;
        }
        for (Map<String, Object> dato : datos) {
            List<Object> fila = new ArrayList<>(claves.size());
            for (String clave : claves) {
                fila.add(dato.get(clave));
            }
            filas.add(fila);
        }
        return filas;
    }
}
