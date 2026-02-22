package com.example.contentieux_security.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class RisqueRequest {
    private BigDecimal montantInitial;
    private String typeGarantie;
    private String descriptionGarantie;
}
