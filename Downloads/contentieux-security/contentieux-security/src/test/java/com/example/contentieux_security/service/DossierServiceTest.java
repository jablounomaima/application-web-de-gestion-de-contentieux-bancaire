package com.example.contentieux_security.service;

import com.example.contentieux_security.entity.*;
import com.example.contentieux_security.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DossierServiceTest {

    @Mock
    private DossierRepository dossierRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private AgenceRepository agenceRepository;

    @InjectMocks
    private DossierService dossierService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testDoubleValidationLogic() {
        // Arrange
        Long dossierId = 1L;
        Dossier dossier = Dossier.builder()
                .id(dossierId)
                .statut(DossierStatus.ATTENTE_VALIDATION)
                .build();

        when(dossierRepository.findById(dossierId)).thenReturn(Optional.of(dossier));

        // Act & Assert 1: Only Financial Validation
        dossierService.validateFinanciere(dossierId);
        assertNotNull(dossier.getValidationFinanciere());
        assertTrue(dossier.getValidationFinanciere().getEstValide());
        assertNull(dossier.getValidationJuridique());
        assertEquals(DossierStatus.ATTENTE_VALIDATION, dossier.getStatut());

        // Act & Assert 2: Only Legal Validation (reset first)
        dossier.setValidationFinanciere(null);
        dossierService.validateJuridique(dossierId);
        assertNull(dossier.getValidationFinanciere());
        assertNotNull(dossier.getValidationJuridique());
        assertTrue(dossier.getValidationJuridique().getEstValide());
        assertEquals(DossierStatus.ATTENTE_VALIDATION, dossier.getStatut());

        // Act & Assert 3: Both Valdiations
        dossier.setValidationFinanciere(ValidationFinanciere.builder().estValide(true).build());
        dossierService.validateJuridique(dossierId); // juridiques will trigger checkFinalValidation
        assertEquals(DossierStatus.VALIDE, dossier.getStatut());
        verify(dossierRepository, atLeastOnce()).save(dossier);
    }
}
