package com.lealtixservice.service;

import com.lealtixservice.dto.caja.TicketPrecuentaDTO;
import java.util.List;

public interface ThermalPrinterService {

    /**
     * Imprime silenciosamente un ticket en la impresora térmica (ESC/POS 58mm).
     *
     * @param ticket DTO del ticket a imprimir
     * @param businessName Nombre del restaurante / negocio
     * @param businessAddress Dirección física (opcional)
     * @param businessPhone Teléfono de contacto (opcional)
     * @param printerNameOverride Nombre específico de impresora (opcional, busca POS-58 por defecto)
     * @return true si el trabajo fue encolado con éxito en el hardware
     */
    boolean printTicket(TicketPrecuentaDTO ticket, String businessName, String businessAddress, String businessPhone, String printerNameOverride);

    /**
     * Imprime silenciosamente un ticket en la impresora térmica (ESC/POS 58mm) con logo del negocio y QR de facturación.
     *
     * @param ticket DTO del ticket a imprimir
     * @param businessName Nombre del restaurante / negocio
     * @param businessAddress Dirección física (opcional)
     * @param businessPhone Teléfono de contacto (opcional)
     * @param logoUrl URL o base64 del logotipo del restaurante (opcional)
     * @param qrFacturacionUrl URL de facturación para generar código QR en el ticket (opcional)
     * @param printerNameOverride Nombre específico de impresora (opcional, busca POS-58 por defecto)
     * @return true si el trabajo fue encolado con éxito en el hardware
     */
    boolean printTicket(TicketPrecuentaDTO ticket, String businessName, String businessAddress, String businessPhone, String logoUrl, String qrFacturacionUrl, String printerNameOverride);

    /**
     * Retorna el nombre de la impresora térmica detectada en el sistema.
     */
    String getActivePrinterName(String printerNameOverride);

    /**
     * Lista todas las impresoras instaladas en el sistema operativo.
     */
    List<String> getInstalledPrinters();
}
