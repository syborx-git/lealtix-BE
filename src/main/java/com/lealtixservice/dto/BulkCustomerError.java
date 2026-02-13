package com.lealtixservice.dto;

import lombok.Builder;
import lombok.Data;

/**
 * DTO que representa un error al procesar un cliente en bulk upload
 */
@Data
@Builder
public class BulkCustomerError {
    private int indice;
    private String mensaje;
}
