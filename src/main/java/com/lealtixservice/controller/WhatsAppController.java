package com.lealtixservice.controller;

import com.lealtixservice.dto.GenericResponse;
import com.lealtixservice.dto.WhatsAppMessageRequest;
import com.lealtixservice.dto.WhatsAppMessageResponse;
import com.lealtixservice.exception.WhatsAppException;
import com.lealtixservice.service.IWhatsAppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/whatsapp")
@Tag(name = "WhatsApp", description = "Endpoints para integración con WhatsApp Cloud API")
public class WhatsAppController {

    private static final Logger logger = LoggerFactory.getLogger(WhatsAppController.class);

    @Autowired
    private IWhatsAppService whatsAppService;

    @Operation(summary = "Enviar mensaje de plantilla WhatsApp")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mensaje enviado exitosamente",
                    content = @Content(schema = @Schema(implementation = GenericResponse.class))),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida (teléfono inválido, datos faltantes)",
                    content = @Content(schema = @Schema(implementation = GenericResponse.class))),
            @ApiResponse(responseCode = "500", description = "Error al enviar el mensaje",
                    content = @Content(schema = @Schema(implementation = GenericResponse.class)))
    })
    @PostMapping("/send-template")
    public ResponseEntity<GenericResponse> sendTemplateMessage(@Valid @RequestBody WhatsAppMessageRequest request) {
        try {
            logger.info("Received WhatsApp template message request for phone: {}", request.getPhoneNumber());

            WhatsAppMessageResponse response = whatsAppService.sendTemplateMessage(request);

            return ResponseEntity.ok(new GenericResponse(
                    200,
                    "Mensaje WhatsApp enviado exitosamente",
                    response
            ));

        } catch (WhatsAppException e) {
            logger.error("WhatsApp exception: {} (Code: {})", e.getMessage(), e.getErrorCode(), e);

            int statusCode = e.getHttpStatusCode() != null ? e.getHttpStatusCode() : 500;
            HttpStatus httpStatus = HttpStatus.valueOf(statusCode);

            return ResponseEntity.status(httpStatus).body(new GenericResponse(
                    statusCode,
                    e.getMessage(),
                    null
            ));

        } catch (Exception e) {
            logger.error("Unexpected error sending WhatsApp message", e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new GenericResponse(
                    500,
                    "Error inesperado al enviar mensaje WhatsApp: " + e.getMessage(),
                    null
            ));
        }
    }
}
