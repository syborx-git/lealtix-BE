package com.lealtixservice.controller;

import com.lealtixservice.dto.GenericResponse;
import com.lealtixservice.service.FacturapiService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/facturapi")
public class FacturapiController {

    @Autowired
    private FacturapiService facturapiService;

    @Operation(summary = "Crear factura (CFDI) vía Facturapi", description = "Reenvía el payload a Facturapi y regresa la factura generada.")
    @PostMapping("/invoices")
    public ResponseEntity<?> createInvoice(@RequestBody Map<String, Object> payload) {
        try {
            Map<String, Object> invoice = facturapiService.createInvoice(payload);
            return ResponseEntity.ok(invoice);
        } catch (Exception e) {
            log.error("Error creando factura en Facturapi", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new GenericResponse(400, e.getMessage(), null));
        }
    }

    @Operation(summary = "Listar facturas de Facturapi", description = "Regresa las facturas generadas en Facturapi.")
    @GetMapping("/invoices")
    public ResponseEntity<?> listInvoices() {
        try {
            List<Map<String, Object>> invoices = facturapiService.listInvoices();
            return ResponseEntity.ok(invoices);
        } catch (Exception e) {
            log.error("Error listando facturas de Facturapi", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new GenericResponse(400, e.getMessage(), null));
        }
    }

    @Operation(summary = "Descargar PDF de factura", description = "Descarga el PDF de la factura.")
    @GetMapping("/invoices/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable String id) {
        byte[] pdf = facturapiService.downloadInvoice(id, "pdf");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", id + ".pdf");
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    @Operation(summary = "Descargar XML de factura", description = "Descarga el XML de la factura.")
    @GetMapping("/invoices/{id}/xml")
    public ResponseEntity<byte[]> downloadXml(@PathVariable String id) {
        byte[] xml = facturapiService.downloadInvoice(id, "xml");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        headers.setContentDispositionFormData("attachment", id + ".xml");
        return new ResponseEntity<>(xml, headers, HttpStatus.OK);
    }
}
