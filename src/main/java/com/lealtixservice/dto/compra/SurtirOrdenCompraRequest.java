package com.lealtixservice.dto.compra;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SurtirOrdenCompraRequest {
    private Long tenantId;
    private String notas;
    private List<SurtirItemRequest> items;
}

