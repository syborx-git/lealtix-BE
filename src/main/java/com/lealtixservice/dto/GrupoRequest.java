package com.lealtixservice.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrupoRequest {
    private List<Long> mesaIds;
}