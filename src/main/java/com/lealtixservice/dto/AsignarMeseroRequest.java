package com.lealtixservice.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsignarMeseroRequest {
    private Long meseroUserId;
}