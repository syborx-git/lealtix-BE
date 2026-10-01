package com.lealtixservice.dto.caja;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableroCajaDTO {
    private List<ComandaCajaRowDTO> cuentasAbiertas;
    private List<ComandaCajaRowDTO> cuentasPorCobrar;
}
