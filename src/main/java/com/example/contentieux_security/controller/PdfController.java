package com.example.contentieux_security.controller;

import com.example.contentieux_security.dto.DossierDetailDTO;
import com.example.contentieux_security.service.DossierService;
import com.example.contentieux_security.service.PdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class PdfController {

    private final DossierService dossierService;
    private final PdfService     pdfService;

    @GetMapping("/agent/dossiers/{id}/pdf")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public ResponseEntity<byte[]> telechargerPdf(@PathVariable Long id,
                                                  Principal principal) {
        try {
            DossierDetailDTO dossier = dossierService.getDossierDetail(id);

            // ✅ Vérifier que les deux validations sont accordées
            if (dossier.getValidationFinanciere() == null
                    || !dossier.getValidationFinanciere()
                    || dossier.getValidationJuridique() == null
                    || !dossier.getValidationJuridique()) {
                return ResponseEntity
                        .badRequest()
                        .body(null);
            }

            byte[] pdf = pdfService.genererDossierPdf(dossier);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"dossier-"
                            + dossier.getNumeroDossier() + ".pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);

        } catch (Exception e) {
            System.err.println("=== ERREUR génération PDF : " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}