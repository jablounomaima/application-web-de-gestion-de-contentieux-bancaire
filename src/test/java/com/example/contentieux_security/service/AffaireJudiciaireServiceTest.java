package com.example.contentieux_security.service;

import com.example.contentieux_security.entity.AffaireJudiciaire;
import com.example.contentieux_security.entity.DossierContentieux;
import com.example.contentieux_security.entity.Prestataire;
import com.example.contentieux_security.enums.TypePrestataire;
import com.example.contentieux_security.repository.AffaireJudiciaireRepository;
import com.example.contentieux_security.repository.AudienceRepository;
import com.example.contentieux_security.repository.DocumentAffaireRepository;
import com.example.contentieux_security.repository.DocumentAffaireRepository;
import com.example.contentieux_security.repository.PrestataireRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AffaireJudiciaireServiceTest {

    @Mock
    private AffaireJudiciaireRepository affaireRepo;

    @Mock
    private AudienceRepository audienceRepo;

    @Mock
    private DocumentAffaireRepository documentRepo;


    @Mock
    private EntityManager entityManager;

    @Mock
    private PrestataireRepository prestataireRepository;

    @InjectMocks
    private AffaireJudiciaireService service;

    @Test
    void shouldReassignAvocatOnAffaireAndMission() {
        DossierContentieux dossier = new DossierContentieux();
        dossier.setId(1L);

        Prestataire ancienAvocat = new Prestataire();
        ancienAvocat.setId(10L);
        ancienAvocat.setUsername("ancien.avocat");
        ancienAvocat.setType(TypePrestataire.AVOCAT);

        Prestataire nouveauAvocat = new Prestataire();
        nouveauAvocat.setId(11L);
        nouveauAvocat.setUsername("nouveau.avocat");
        nouveauAvocat.setType(TypePrestataire.AVOCAT);


        AffaireJudiciaire affaire = new AffaireJudiciaire();
        affaire.setId(42L);
        affaire.setDossier(dossier);
        affaire.setAvocat(ancienAvocat);

        when(affaireRepo.findByDossier_Id(1L)).thenReturn(List.of(affaire));
        when(prestataireRepository.findById(11L)).thenReturn(Optional.of(nouveauAvocat));
        when(affaireRepo.save(affaire)).thenReturn(affaire);

        AffaireJudiciaire result = service.reassignerAvocatPourDossier(1L, 11L);

        assertThat(result.getAvocat()).isSameAs(nouveauAvocat);
        verify(affaireRepo).save(affaire);
    }
}
