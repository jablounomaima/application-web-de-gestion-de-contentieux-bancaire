package com.example.contentieux_security.service;

import com.example.contentieux_security.entity.*;
import com.example.contentieux_security.enums.DossierStatus;
import com.example.contentieux_security.enums.StatutMission;     // ← enums/
import com.example.contentieux_security.enums.StatutPrestation;  // ← enums/
import com.example.contentieux_security.enums.TypePrestataire;   // ← enums/
import com.example.contentieux_security.enums.TypePrestation;    // ← enums/
import com.example.contentieux_security.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PrestationService {

    private final PrestationRepository    prestationRepository;
    private final MissionRepository       missionRepository;
    private final DossierRepository       dossierRepository;
    private final PrestataireRepository   prestataireRepository;
    private final AgentBancaireRepository agentRepository;
    private final HistoriqueService       historiqueService;
    private final NotificationService     notificationService;

    // ════════════════════════════════════════════════════
    //  PRESTATION
    // ════════════════════════════════════════════════════

    @Transactional
    public Prestation lancerPrestation(Long dossierId, TypePrestation type,
                                       String description, String agentUsername) {

        DossierContentieux dossier = dossierRepository.findById(dossierId)
                .orElseThrow(() -> new RuntimeException("Dossier introuvable"));

        AgentBancaire agent = agentRepository.findByUsername(agentUsername)
                .orElseThrow(() -> new RuntimeException("Agent introuvable"));

        if (type == TypePrestation.PROCEDURE_JUDICIAIRE
                && dossier.getStatut() != DossierStatus.VALIDE) {
            throw new RuntimeException(
                "Le dossier doit être validé avant de lancer une procédure judiciaire.");
        }

        Prestation prestation = Prestation.builder()
                .numeroPrestation(genererNumeroPrestation())
                .type(type)
                .statut(StatutPrestation.EN_COURS)
                .description(description)
                .dossier(dossier)
                .agentCreateur(agent)
                .dateCreation(LocalDateTime.now())
                .build();

        prestation = prestationRepository.save(prestation);

        if (type == TypePrestation.PROCEDURE_JUDICIAIRE) {
            dossier.setStatut(DossierStatus.EN_PROCEDURE);
        } else if (type == TypePrestation.EXECUTION_FORCEE) {
            dossier.setStatut(DossierStatus.EN_EXECUTION);
        }
        dossierRepository.save(dossier);

        historiqueService.enregistrer(dossier, "PRESTATION_LANCEE",
                "Prestation lancée : " + type.getLibelle(), agentUsername);

        return prestation;
    }

    public List<Prestation> getPrestationsDossier(Long dossierId) {
        return prestationRepository.findByDossierIdOrderByDateDesc(dossierId);
    }

    public Prestation getPrestationById(Long id) {
        return prestationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Prestation introuvable"));
    }

    // ════════════════════════════════════════════════════
    //  MISSION
    // ════════════════════════════════════════════════════

    @Transactional
    public Mission designerPrestataire(Long prestationId, Long prestataireId,
                                        String description, LocalDate dateFinPrevue,
                                        String agentUsername) {

        Prestation prestation = getPrestationById(prestationId);

        Prestataire prestataire = prestataireRepository.findById(prestataireId)
                .orElseThrow(() -> new RuntimeException("Prestataire introuvable"));

        Mission mission = Mission.builder()
                .numeroMission(genererNumeroMission())
                .description(description)
                .statut(StatutMission.ASSIGNEE)
                .prestation(prestation)
                .prestataire(prestataire)
                .dateAssignation(LocalDateTime.now())
                .dateFinPrevue(dateFinPrevue)
                .build();

        mission = missionRepository.save(mission);

        notificationService.notifier(
                prestataire.getUsername(),
                "Nouvelle mission assignée",
                "Une mission vous a été assignée pour le dossier "
                + prestation.getDossier().getNumeroDossier(),
                "MISSION",
                prestation.getDossier()
        );

        historiqueService.enregistrer(prestation.getDossier(), "MISSION_ASSIGNEE",
                prestataire.getType().name() + " désigné : "
                + prestataire.getNom() + " " + prestataire.getPrenom(),
                agentUsername);

        return mission;
    }

    public List<Mission> getMissionsByPrestation(Long prestationId) {
        return missionRepository.findByPrestation_Id(prestationId);
    }

    public List<Mission> getMissionsPrestataire(String username) {
        return missionRepository.findByPrestataire_Username(username);
    }

    public Mission getMissionById(Long id) {
        return missionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mission introuvable"));
    }

    @Transactional
    public void soumettrePV(Long missionId, String pvTexte, String username) {
        Mission mission = getMissionById(missionId);
        mission.setPvMission(pvTexte);
        mission.setStatut(StatutMission.PV_SOUMIS);
        missionRepository.save(mission);

        notificationService.notifier(
                mission.getPrestation().getDossier().getAgentCreateur().getUsername(),
                "PV de mission soumis",
                "Le prestataire " + mission.getPrestataire().getNom()
                + " a soumis un PV pour le dossier "
                + mission.getPrestation().getDossier().getNumeroDossier(),
                "PV_MISSION",
                mission.getPrestation().getDossier()
        );

        historiqueService.enregistrer(mission.getPrestation().getDossier(),
                "PV_SOUMIS", "PV soumis par " + username, username);
    }

    @Transactional
    public void soumettreFacture(Long missionId, Double montant,
                                  String factureRef, String username) {
        Mission mission = getMissionById(missionId);
        mission.setMontantFacture(montant);
        mission.setFactureRef(factureRef);
        mission.setStatut(StatutMission.FACTURE_SOUMISE);
        missionRepository.save(mission);

        notificationService.notifier(
                mission.getPrestation().getDossier().getAgentCreateur().getUsername(),
                "Facture soumise",
                "Le prestataire " + mission.getPrestataire().getNom()
                + " a soumis une facture de " + montant + " DT",
                "FACTURE",
                mission.getPrestation().getDossier()
        );

        historiqueService.enregistrer(mission.getPrestation().getDossier(),
                "FACTURE_SOUMISE",
                "Facture soumise : " + montant + " DT — ref: " + factureRef,
                username);
    }

    // ════════════════════════════════════════════════════
    //  UTILITAIRE
    // ════════════════════════════════════════════════════

    private String genererNumeroPrestation() {
        String prefix = "PREST-" + LocalDate.now().getYear();
        Optional<String> last = prestationRepository.findLastNumero(prefix);
        int seq = 1;
        if (last.isPresent()) {
            try {
                String[] parts = last.get().split("-");
                seq = Integer.parseInt(parts[parts.length - 1]) + 1;
            } catch (Exception ignored) {}
        }
        return String.format("%s-%05d", prefix, seq);
    }

    private String genererNumeroMission() {
        String prefix = "MISS-" + LocalDate.now().getYear();
        Optional<String> last = missionRepository.findLastNumero(prefix);
        int seq = 1;
        if (last.isPresent()) {
            try {
                String[] parts = last.get().split("-");
                seq = Integer.parseInt(parts[parts.length - 1]) + 1;
            } catch (Exception ignored) {}
        }
        return String.format("%s-%05d", prefix, seq);
    }
}