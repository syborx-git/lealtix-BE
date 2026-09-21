package com.lealtixservice.service;

import java.util.List;
import java.util.Map;

public interface FacturapiService {
    Map<String, Object> createInvoice(Map<String, Object> payload);

    List<Map<String, Object>> listInvoices();

    byte[] downloadInvoice(String invoiceId, String format);
}
