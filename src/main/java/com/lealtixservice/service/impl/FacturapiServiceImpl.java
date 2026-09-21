package com.lealtixservice.service.impl;

import com.lealtixservice.service.FacturapiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class FacturapiServiceImpl implements FacturapiService {

    private static final String FACTURAPI_BASE_URL = "https://www.facturapi.io/v2";

    @Value("${facturapi.api.key}")
    private String apiKey;

    @Value("${facturapi.default.product.key:90111801}")
    private String defaultProductKey;

    @Value("${facturapi.default.unit.key:H87}")
    private String defaultUnitKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public Map<String, Object> createInvoice(Map<String, Object> payload) {
        String customerId = createCustomer(payload.get("customer"));
        return createInvoiceWithCustomer(customerId, payload);
    }

    @SuppressWarnings("unchecked")
    private String createCustomer(Object customerObj) {
        Map<String, Object> customer = customerObj instanceof Map ? (Map<String, Object>) customerObj : new HashMap<>();

        Map<String, Object> body = new HashMap<>();
        body.put("legal_name", customer.get("legalName"));
        body.put("tax_id", customer.get("taxId"));
        body.put("tax_system", customer.get("taxSystem"));
        body.put("email", customer.get("email"));
        body.put("address", customer.get("address") != null ? customer.get("address") : defaultAddress());

        HttpHeaders headers = authHeaders();
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    FACTURAPI_BASE_URL + "/customers", entity, Map.class);
            Map<String, Object> created = response.getBody();
            return (String) created.get("id");
        } catch (HttpClientErrorException e) {
            log.error("Facturapi error al crear cliente: {}", e.getResponseBodyAsString());
            throw new RuntimeException("Facturapi error: " + e.getResponseBodyAsString());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> createInvoiceWithCustomer(String customerId, Map<String, Object> payload) {
        List<Map<String, Object>> rawItems = payload.get("items") instanceof List
                ? (List<Map<String, Object>>) payload.get("items")
                : new ArrayList<>();

        List<Map<String, Object>> items = new ArrayList<>();
        for (Map<String, Object> item : rawItems) {
            Map<String, Object> product = new HashMap<>();
            product.put("description", item.get("description"));
            product.put("product_key", item.getOrDefault("productKey", defaultProductKey));
            product.put("unit_key", item.getOrDefault("unitKey", defaultUnitKey));
            product.put("price", item.get("price"));
            product.put("tax_included", true);

            Map<String, Object> line = new HashMap<>();
            line.put("quantity", item.getOrDefault("quantity", 1));
            line.put("product", product);
            items.add(line);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("customer", customerId);
        body.put("items", items);
        body.put("payment_form", payload.getOrDefault("paymentForm", "04"));
        body.put("use", payload.getOrDefault("use", "G03"));
        body.put("currency", payload.getOrDefault("currency", "MXN"));
        if (payload.get("externalId") != null) {
            body.put("external_id", payload.get("externalId"));
        }

        HttpHeaders headers = authHeaders();
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    FACTURAPI_BASE_URL + "/invoices", entity, Map.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Facturapi error al crear factura: {}", e.getResponseBodyAsString());
            throw new RuntimeException("Facturapi error: " + e.getResponseBodyAsString());
        }
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        return headers;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listInvoices() {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    FACTURAPI_BASE_URL + "/invoices?limit=100",
                    org.springframework.http.HttpMethod.GET,
                    entity,
                    Map.class
            );
            Map<String, Object> body = response.getBody();
            if (body == null) return new ArrayList<>();
            Object data = body.get("data");
            return data instanceof List ? (List<Map<String, Object>>) data : new ArrayList<>();
        } catch (HttpClientErrorException e) {
            log.error("Facturapi error al listar facturas: {}", e.getResponseBodyAsString());
            throw new RuntimeException("Facturapi error: " + e.getResponseBodyAsString());
        }
    }

    @Override
    public byte[] downloadInvoice(String invoiceId, String format) {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    FACTURAPI_BASE_URL + "/invoices/" + invoiceId + "/" + format,
                    org.springframework.http.HttpMethod.GET,
                    entity,
                    byte[].class
            );
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Facturapi error al descargar {} de factura {}: {}", format, invoiceId, e.getResponseBodyAsString());
            throw new RuntimeException("Facturapi error: " + e.getResponseBodyAsString());
        }
    }

    private Map<String, Object> defaultAddress() {
        Map<String, Object> address = new HashMap<>();
        address.put("street", "Calle Prueba");
        address.put("exterior", "100");
        address.put("neighborhood", "Centro");
        address.put("city", "Metepec");
        address.put("municipality", "Metepec");
        address.put("zip", "52140");
        address.put("state", "México");
        address.put("country", "MEX");
        return address;
    }
}
