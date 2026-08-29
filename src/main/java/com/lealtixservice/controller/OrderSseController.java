package com.lealtixservice.controller;

import com.lealtixservice.service.OrderSseService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Tag(name = "SSE Orders", description = "Server-Sent Events para notificaciones de órdenes en tiempo real")
@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
public class OrderSseController {

    private final OrderSseService orderSseService;

    @GetMapping(value = "/orders", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> streamOrders(@RequestParam Long tenantId) {
        log.info("[SSE] Nueva suscripción SSE para tenant {}", tenantId);
        SseEmitter emitter = orderSseService.subscribe(tenantId);

        // Headers críticos para que proxies/nginx/cloudflare NO buffericen el stream
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_EVENT_STREAM);
        headers.setCacheControl("no-cache, no-transform");
        headers.set("X-Accel-Buffering", "no");
        headers.set("Connection", "keep-alive");

        return new ResponseEntity<>(emitter, headers, HttpStatus.OK);
    }
}
