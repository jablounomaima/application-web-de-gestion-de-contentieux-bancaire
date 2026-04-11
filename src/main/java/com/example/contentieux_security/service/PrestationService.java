package com.example.contentieux_security.service;

import com.example.contentieux_security.entity.*;
import com.example.contentieux_security.enums.DossierStatus;
import com.example.contentieux_security.enums.StatutMission;
import com.example.contentieux_security.enums.StatutPrestation;
import com.example.contentieux_security.enums.TypePrestation;
import com.example.contentieux_security.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.contentieux_security.repository.AgentBancaireRepository;
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
    private final HistoriqueService       historiqueService;
    private final NotificationService     notificationService;
    private final AgentBancaireRepository agentBancaireRepository;

    // ════════════════════════════════════════════════════
    //  PRESTATION
    // ════════════════════════════════════════════════════

    @Transactional
    public Prestation lancerPrestation(Long dossierId, TypePrestation type,
                                       String description, String agentUsername) {

        DossierContentieux dossier = dossierRepository.findById(dossierId)
                .orElseThrow(() -> new IllegalArgumentException("Dossier introuvable : id=" + dossierId));

                AgentBancaire agent = agentBancaireRepository.findByUsername(agentUsername)
                .orElseThrow(() -> new IllegalArgumentException("Agent introuvable : " + agentUsername));

        // ── Vérifications métier ──────────────────────────────────────────────
        if (type == TypePrestation.PROCEDURE_JUDICIAIRE
                && dossier.getStatut() != DossierStatus.VALIDE) {
            throw new IllegalStateException(
                "Le dossier doit être au statut VALIDE pour lancer une procédure judiciaire. "
                + "Statut actuel : " + dossier.getStatut());
        }

        if (type == TypePrestation.EXECUTION_FORCEE
                && dossier.getStatut() != DossierStatus.EN_PROCEDURE) {
            throw new IllegalStateException(
                "L'exécution forcée nécessite que le dossier soit EN_PROCEDURE. "
                + "Statut actuel : " + dossier.getStatut());
        }

        // ── Création prestation ───────────────────────────────────────────────
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

        // ── Mise à jour statut dossier ────────────────────────────────────────
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
                .orElseThrow(() -> new IllegalArgumentException("Prestation introuvable : id=" + id));
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
                .orElseThrow(() -> new IllegalArgumentException("Prestataire introuvable : id=" + prestataireId));

        // ── Vérification prestataire actif ────────────────────────────────────
        if (!prestataire.isActif()) {
            throw new IllegalStateException(
                "Le prestataire " + prestataire.getNom() + " n'est plus actif.");
        }

        // ── Vérification cohérence type prestataire / type prestation ─────────
        validerCompatibiliteTypePrestataire(prestation.getType(), prestataire);

        // ── Création mission ──────────────────────────────────────────────────
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

        // ── Notification prestataire ──────────────────────────────────────────
        notificationService.notifier(
                prestataire.getUsername(),
                "Nouvelle mission assignée",
                "Une mission vous a été assignée pour le dossier "
                + prestation.getDossier().getNumeroDossier()
                + " — Mission : " + mission.getNumeroMission(),
                "MISSION",
                prestation.getDossier()
        );

        historiqueService.enregistrer(
                prestation.getDossier(),
                "MISSION_ASSIGNEE",
                prestataire.getType().name() + " désigné : "
                + prestataire.getNom() + " " + prestataire.getPrenom(),
                agentUsername);

        return mission;
    }

    public List<Mission> getMissionsByPrestation(Long prestationId) {
        // Vérifier que la prestation existe
        getPrestationById(prestationId);
        return missionRepository.findByPrestation_Id(prestationId);
    }

    public List<Mission> getMissionsPrestataire(String username) {

        // Si c'est un prestataire (avocat, huissier, expert)
        if (prestataireRepository.findByUsername(username).isPresent()) {
            return missionRepository.findMissionsWithDetails(username);
        }
    
        // Si c'est un agent bancaire
        if (agentBancaireRepository.findByUsername(username).isPresent()) {
            return missionRepository.findMissionsByAgentUsername(username);
        }
    
        return List.of();
    }
    public Mission getMissionById(Long id) {
        return missionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mission introuvable : id=" + id));
    }

    @Transactional
    public void soumettrePV(Long missionId, String pvTexte, String username) {

        Mission mission = getMissionById(missionId);

        // ── Vérifications ─────────────────────────────────────────────────────
        if (!mission.getPrestataire().getUsername().equals(username)) {
            throw new IllegalStateException("Vous n'êtes pas autorisé à soumettre le PV de cette mission.");
        }

        if (mission.getStatut() != StatutMission.ASSIGNEE
                && mission.getStatut() != StatutMission.EN_COURS) {
            throw new IllegalStateException(
                "Impossible de soumettre un PV pour une mission au statut : " + mission.getStatut());
        }

        if (pvTexte == null || pvTexte.isBlank()) {
            throw new IllegalArgumentException("Le texte du PV ne peut pas être vide.");
        }

        mission.setPvMission(pvTexte);
        mission.setStatut(StatutMission.PV_SOUMIS);
        missionRepository.save(mission);

        // ── Notification agent ────────────────────────────────────────────────
        notificationService.notifier(
                mission.getPrestation().getDossier().getAgentCreateur().getUsername(),
                "PV de mission soumis — " + mission.getNumeroMission(),
                "Le prestataire " + mission.getPrestataire().getNom()
                + " a soumis un PV pour le dossier "
                + mission.getPrestation().getDossier().getNumeroDossier(),
                "PV_MISSION",
                mission.getPrestation().getDossier()
        );

        historiqueService.enregistrer(
                mission.getPrestation().getDossier(),
                "PV_SOUMIS",
                "PV soumis par " + username + " — mission " + mission.getNumeroMission(),
                username);
    }

    @Transactional
    public void soumettreFacture(Long missionId, Double montant,
                                  String factureRef, String username) {

        Mission mission = getMissionById(missionId);

        // ── Vérifications ─────────────────────────────────────────────────────
        if (!mission.getPrestataire().getUsername().equals(username)) {
            throw new IllegalStateException("Vous n'êtes pas autorisé à soumettre la facture de cette mission.");
        }

        if (mission.getStatut() != StatutMission.PV_SOUMIS) {
            throw new IllegalStateException(
                "La facture ne peut être soumise qu'après le PV. Statut actuel : " + mission.getStatut());
        }

        if (montant == null || montant <= 0) {
            throw new IllegalArgumentException("Le montant de la facture doit être supérieur à 0.");
        }

        if (factureRef == null || factureRef.isBlank()) {
            throw new IllegalArgumentException("La référence de la facture est obligatoire.");
        }

        mission.setMontantFacture(montant);
        mission.setFactureRef(factureRef);
        mission.setStatut(StatutMission.FACTURE_SOUMISE);
        missionRepository.save(mission);

        // ── Notification agent ────────────────────────────────────────────────
        notificationService.notifier(
                mission.getPrestation().getDossier().getAgentCreateur().getUsername(),
                "Facture soumise — " + mission.getNumeroMission(),
                "Le prestataire " + mission.getPrestataire().getNom()
                + " a soumis une facture de " + montant + " TND"
                + " pour le dossier "
                + mission.getPrestation().getDossier().getNumeroDossier(),
                "FACTURE",
                mission.getPrestation().getDossier()
        );

        historiqueService.enregistrer(
                mission.getPrestation().getDossier(),
                "FACTURE_SOUMISE",
                "Facture soumise : " + montant + " TND — réf : " + factureRef
                + " — mission " + mission.getNumeroMission(),
                username);
    }

    // ════════════════════════════════════════════════════
    //  UTILITAIRES PRIVÉS
    // ════════════════════════════════════════════════════

    /**
     * Vérifie que le type de prestataire est cohérent avec le type de prestation.
     * PROCEDURE_JUDICIAIRE → AVOCAT uniquement
     * EXECUTION_FORCEE     → EXPERT ou HUISSIER
     */
    private void validerCompatibiliteTypePrestataire(TypePrestation typePrestation,
                                                      Prestataire prestataire) {
        switch (typePrestation) {
            case PROCEDURE_JUDICIAIRE -> {
                if (prestataire.getType() != com.example.contentieux_security.enums.TypePrestataire.AVOCAT) {
                    throw new IllegalStateException(
                        "Une procédure judiciaire requiert un AVOCAT. "
                        + "Type sélectionné : " + prestataire.getType());
                }
            }
            case EXECUTION_FORCEE -> {
                var type = prestataire.getType();
                if (type != com.example.contentieux_security.enums.TypePrestataire.EXPERT
                        && type != com.example.contentieux_security.enums.TypePrestataire.HUISSIER) {
                    throw new IllegalStateException(
                        "Une exécution forcée requiert un EXPERT ou un HUISSIER. "
                        + "Type sélectionné : " + type);
                }
            }
        }
    }

    private String genererNumeroPrestation() {
        String prefix = "PREST-" + LocalDate.now().getYear();
        Optional<String> last = prestationRepository.findLastNumero(prefix);
        int seq = 1;
        if (last.isPresent()) {
            try {
                String[] parts = last.get().split("-");
                seq = Integer.parseInt(parts[parts.length - 1]) + 1;
            } catch (NumberFormatException ignored) {}
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
            } catch (NumberFormatException ignored) {}
        }
        return String.format("%s-%05d", prefix, seq);
    }



    public boolean deletePrestataire(Long id, String username) {
        Prestataire prestataire = prestataireRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Prestataire non trouvé"));

        // (optionnel) vérification utilisateur
        if (!prestataire.getAgentResponsable().getUsername().equals(username)) {            throw new RuntimeException("Accès refusé");
        }

        prestataireRepository.delete(prestataire);
        return true;
    }


    public Mission getMissionByIdWithDetails(Long id) {
        return missionRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Mission introuvable"));
    }

}