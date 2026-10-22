package com.lealtixservice.service.impl;

import com.lealtixservice.dto.caja.ItemPrecuentaDTO;
import com.lealtixservice.dto.caja.TicketPrecuentaDTO;
import com.lealtixservice.service.ThermalPrinterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.print.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
public class ThermalPrinterServiceImpl implements ThermalPrinterService {

    private static final int LINE_WIDTH = 32; // Estándar 58mm (fuente A: 32 columnas)
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a", Locale.ENGLISH);

    // Comandos ESC/POS
    private static final byte[] ESC_INIT = new byte[]{0x1B, 0x40};               // ESC @ : Inicializar
    private static final byte[] ESC_ALIGN_LEFT = new byte[]{0x1B, 0x61, 0x00};   // ESC a 0
    private static final byte[] ESC_ALIGN_CENTER = new byte[]{0x1B, 0x61, 0x01}; // ESC a 1
    private static final byte[] ESC_ALIGN_RIGHT = new byte[]{0x1B, 0x61, 0x02};  // ESC a 2
    private static final byte[] ESC_BOLD_ON = new byte[]{0x1B, 0x45, 0x01};      // ESC E 1
    private static final byte[] ESC_BOLD_OFF = new byte[]{0x1B, 0x45, 0x00};     // ESC E 0
    private static final byte[] GS_DOUBLE_ON = new byte[]{0x1D, 0x21, 0x11};     // GS ! 17 (Doble alto y ancho)
    private static final byte[] GS_NORMAL = new byte[]{0x1D, 0x21, 0x00};        // GS ! 0 (Normal)
    private static final byte[] FEED_AND_CUT = new byte[]{0x0A, 0x0A, 0x0A, 0x0A, 0x1D, 0x56, 0x41, 0x10}; // Avance y corte parcial

    @Override
    public boolean printTicket(TicketPrecuentaDTO ticket, String businessName, String businessAddress, String businessPhone, String printerNameOverride) {
        if (ticket == null) {
            log.warn("No se puede imprimir ticket nulo");
            return false;
        }

        PrintService printService = findTargetPrinter(printerNameOverride);
        if (printService == null) {
            log.warn("No se encontró impresora térmica disponible en el sistema");
            return false;
        }

        try {
            byte[] escposBytes = buildEscPosTicket(ticket, businessName, businessAddress, businessPhone);
            DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;
            Doc doc = new SimpleDoc(escposBytes, flavor, null);
            DocPrintJob printJob = printService.createPrintJob();

            log.info("Enviando ticket comanda {} silenciosamente a impresora [{}]", ticket.getFolioComanda(), printService.getName());
            printJob.print(doc, null);
            return true;
        } catch (Exception e) {
            log.error("Error al enviar trabajo a la impresora [{}] para comanda {}: {}",
                    printService.getName(), ticket.getFolioComanda(), e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String getActivePrinterName(String printerNameOverride) {
        PrintService ps = findTargetPrinter(printerNameOverride);
        return ps != null ? ps.getName() : null;
    }

    @Override
    public List<String> getInstalledPrinters() {
        List<String> list = new ArrayList<>();
        try {
            PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
            for (PrintService ps : services) {
                list.add(ps.getName());
            }
        } catch (Exception e) {
            log.warn("Error listando impresoras: {}", e.getMessage());
        }
        return list;
    }

    private PrintService findTargetPrinter(String overrideName) {
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        if (services == null || services.length == 0) {
            return null;
        }

        // 1. Si especificaron un nombre exacto
        if (overrideName != null && !overrideName.trim().isEmpty()) {
            for (PrintService ps : services) {
                if (ps.getName().equalsIgnoreCase(overrideName.trim())) {
                    return ps;
                }
            }
        }

        // 2. Si la impresora por defecto es POS o 58
        PrintService defaultPs = PrintServiceLookup.lookupDefaultPrintService();
        if (defaultPs != null && isThermalPrinter(defaultPs.getName())) {
            return defaultPs;
        }

        // 3. Buscar la primera que se llame POS-58
        for (PrintService ps : services) {
            if (ps.getName().equalsIgnoreCase("POS-58")) {
                return ps;
            }
        }

        // 4. Buscar cualquier impresora con nombre térmico
        for (PrintService ps : services) {
            if (isThermalPrinter(ps.getName())) {
                return ps;
            }
        }

        // 5. Fallback a la impresora por defecto
        return defaultPs != null ? defaultPs : services[0];
    }

    private boolean isThermalPrinter(String name) {
        if (name == null) return false;
        String upper = name.toUpperCase();
        return upper.contains("POS") || upper.contains("58") || upper.contains("80") ||
                upper.contains("THERMAL") || upper.contains("RECEIPT") || upper.contains("TICKET");
    }

    private byte[] buildEscPosTicket(TicketPrecuentaDTO t, String businessName, String address, String phone) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // 1. Inicializar impresora
        out.write(ESC_INIT);

        // 2. Encabezado Centrado
        out.write(ESC_ALIGN_CENTER);
        out.write(ESC_BOLD_ON);

        String bName = clean(businessName != null && !businessName.trim().isEmpty() ? businessName : "RESTAURANTE").toUpperCase();
        out.write((bName + "\n").getBytes(StandardCharsets.US_ASCII));
        out.write(ESC_BOLD_OFF);

        if (address != null && !address.trim().isEmpty()) {
            out.write((clean(address).toUpperCase() + "\n").getBytes(StandardCharsets.US_ASCII));
        }
        if (phone != null && !phone.trim().isEmpty()) {
            out.write(("Tel: " + clean(phone) + "\n").getBytes(StandardCharsets.US_ASCII));
        }

        boolean esPagada = "PAGADA".equalsIgnoreCase(t.getEstado());
        out.write(ESC_BOLD_ON);
        out.write((esPagada ? "*** COMPROBANTE DE PAGO ***\n" : "*** PRE-CUENTA ***\n").getBytes(StandardCharsets.US_ASCII));
        out.write(ESC_BOLD_OFF);

        out.write((repeat('-', LINE_WIDTH) + "\n").getBytes(StandardCharsets.US_ASCII));

        // 3. Metadatos (Alineado izquierda)
        out.write(ESC_ALIGN_LEFT);
        out.write((padRow("FOLIO:", "#" + t.getFolioComanda()) + "\n").getBytes(StandardCharsets.US_ASCII));
        out.write((padRow("MESA:", clean(t.getMesaNombre() != null ? t.getMesaNombre() : "General")) + "\n").getBytes(StandardCharsets.US_ASCII));
        if (t.getMeseroNombre() != null) {
            out.write((padRow("MESERO:", clean(t.getMeseroNombre())) + "\n").getBytes(StandardCharsets.US_ASCII));
        }
        if (t.getClienteNombre() != null) {
            out.write((padRow("CLIENTE:", clean(t.getClienteNombre())) + "\n").getBytes(StandardCharsets.US_ASCII));
        }
        String fechaStr = t.getFechaImpresion() != null ? t.getFechaImpresion().format(DATE_FORMATTER) : "";
        if (!fechaStr.isEmpty()) {
            out.write((padRow("FECHA:", fechaStr) + "\n").getBytes(StandardCharsets.US_ASCII));
        }

        out.write((repeat('-', LINE_WIDTH) + "\n").getBytes(StandardCharsets.US_ASCII));

        // 4. Tabla de Artículos
        out.write((padRow("CANT / DESCRIPCION", "TOTAL") + "\n").getBytes(StandardCharsets.US_ASCII));
        if (t.getItems() != null) {
            for (ItemPrecuentaDTO it : t.getItems()) {
                String cantDesc = it.getCantidad() + "x " + clean(it.getNombreProducto());
                if (it.getAsientoAlias() != null && !it.getAsientoAlias().trim().isEmpty()) {
                    cantDesc += " (" + clean(it.getAsientoAlias()) + ")";
                }
                String priceStr = "$" + (it.getTotalLinea() != null ? it.getTotalLinea().setScale(2).toString() : "0.00");
                out.write((padItemRow(cantDesc, priceStr) + "\n").getBytes(StandardCharsets.US_ASCII));
            }
        }

        out.write((repeat('-', LINE_WIDTH) + "\n").getBytes(StandardCharsets.US_ASCII));

        // 5. Totales
        if (t.getDescuento() != null && t.getDescuento().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal sub = t.getSubtotal() != null ? t.getSubtotal() : t.getTotal();
            out.write((padRow("SUBTOTAL:", "$" + sub.setScale(2)) + "\n").getBytes(StandardCharsets.US_ASCII));
            out.write((padRow("DESCUENTO:", "-$" + t.getDescuento().setScale(2)) + "\n").getBytes(StandardCharsets.US_ASCII));
        }

        out.write(ESC_BOLD_ON);
        BigDecimal total = t.getTotal() != null ? t.getTotal() : BigDecimal.ZERO;
        out.write((padRow("TOTAL:", "$" + total.setScale(2)) + "\n").getBytes(StandardCharsets.US_ASCII));
        out.write(ESC_BOLD_OFF);

        if (esPagada) {
            out.write((padRow("ESTADO:", "PAGADO [OK]") + "\n").getBytes(StandardCharsets.US_ASCII));
            if (t.getMetodoPago() != null) {
                out.write((padRow("METODO:", clean(t.getMetodoPago())) + "\n").getBytes(StandardCharsets.US_ASCII));
            }
        } else {
            // Propina sugerida
            out.write((repeat('.', LINE_WIDTH) + "\n").getBytes(StandardCharsets.US_ASCII));
            out.write(ESC_ALIGN_CENTER);
            out.write("PROPINA SUGERIDA (OPCIONAL)\n".getBytes(StandardCharsets.US_ASCII));
            out.write(ESC_ALIGN_LEFT);
            BigDecimal p10 = t.getPropinaSugerida10() != null ? t.getPropinaSugerida10() : total.multiply(new BigDecimal("0.10"));
            BigDecimal p15 = t.getPropinaSugerida15() != null ? t.getPropinaSugerida15() : total.multiply(new BigDecimal("0.15"));
            BigDecimal p20 = t.getPropinaSugerida20() != null ? t.getPropinaSugerida20() : total.multiply(new BigDecimal("0.20"));
            out.write((padRow("10%:", "$" + p10.setScale(2)) + "\n").getBytes(StandardCharsets.US_ASCII));
            out.write((padRow("15%:", "$" + p15.setScale(2)) + "\n").getBytes(StandardCharsets.US_ASCII));
            out.write((padRow("20%:", "$" + p20.setScale(2)) + "\n").getBytes(StandardCharsets.US_ASCII));
        }

        out.write((repeat('=', LINE_WIDTH) + "\n").getBytes(StandardCharsets.US_ASCII));

        // 6. Pie de Página Centrado
        out.write(ESC_ALIGN_CENTER);
        out.write("!GRACIAS POR SU PREFERENCIA!\n".getBytes(StandardCharsets.US_ASCII));
        out.write((esPagada ? "Comprobante de Pago\n" : "Este ticket no es comprobante fiscal\n").getBytes(StandardCharsets.US_ASCII));

        // 7. Avance y corte
        out.write(FEED_AND_CUT);

        return out.toByteArray();
    }

    private String clean(String s) {
        if (s == null) return "";
        String normalized = Normalizer.normalize(s, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace("ñ", "n").replace("Ñ", "N")
                .replaceAll("[^\\x20-\\x7E]", ""); // Mantener solo caracteres imprimibles ASCII
    }

    private String repeat(char c, int count) {
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(c);
        }
        return sb.toString();
    }

    private String padRow(String left, String right) {
        int spaceNeeded = LINE_WIDTH - left.length() - right.length();
        if (spaceNeeded < 1) {
            return left + " " + right;
        }
        return left + repeat(' ', spaceNeeded) + right;
    }

    private String padItemRow(String desc, String price) {
        int maxDescLen = LINE_WIDTH - price.length() - 1;
        if (desc.length() > maxDescLen) {
            desc = desc.substring(0, maxDescLen);
        }
        int spaceNeeded = LINE_WIDTH - desc.length() - price.length();
        return desc + repeat(' ', Math.max(1, spaceNeeded)) + price;
    }
}
