package com.lealtixservice.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PosicionRequest {
    private Double posicionX;
    private Double posicionY;
}