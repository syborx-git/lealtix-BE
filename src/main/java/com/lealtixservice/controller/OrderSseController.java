package com.lealtixservice.controller;

import com.lealtixservice.service.OrderSseService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
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
    public SseEmitter streamOrders(@RequestParam Long tenantId) {
        log.info("[SSE] Nueva suscripción SSE para tenant {}", tenantId);
        return orderSseService.subscribe(tenantId);
    }
}
