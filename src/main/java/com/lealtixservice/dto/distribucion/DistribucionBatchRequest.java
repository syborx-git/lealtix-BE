package com.lealtixservice.dto.distribucion;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistribucionBatchRequest {
    private Long tenantId;
    private List<DistribucionBatchItemRequest> items;
}

