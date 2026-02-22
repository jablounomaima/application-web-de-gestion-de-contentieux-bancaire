package com.example.contentieux_security.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class DossierRequest {
    private Long clientId;
    private Long agenceId;
}
