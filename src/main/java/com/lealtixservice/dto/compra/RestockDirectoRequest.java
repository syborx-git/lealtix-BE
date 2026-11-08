package com.lealtixservice.dto.compra;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestockDirectoRequest {
    private Long tenantId;
    private String titulo;
    private String notas;
    private List<SurtirItemRequest> items;
}

