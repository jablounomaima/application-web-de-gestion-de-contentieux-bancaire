package com.example.contentieux_security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {
    private Long    id;
    private String  titre;
    private String  message;
    private String  type;
    private String  dateCreation;   // formatée ISO-8601 côté frontend
    private boolean lue;
    private Integer missionId;  // ← ajouter si absent
    private String  urlAction;
    private Long    dossierId;
}