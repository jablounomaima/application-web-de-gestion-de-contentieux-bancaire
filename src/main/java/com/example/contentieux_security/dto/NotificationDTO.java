package com.example.contentieux_security.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationDTO {
    private Long id;
    private String message;
    private String date;

    private Long dossierId;
    private String numeroDossier;
}