package com.lealtixservice.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BulkCustomerUploadResponse {
    private int exitosos;
    private int fallidos;
    private List<BulkCustomerError> errores;
}
