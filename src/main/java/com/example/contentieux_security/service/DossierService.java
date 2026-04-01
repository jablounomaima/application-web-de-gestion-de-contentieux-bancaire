package com.example.contentieux_security.service;

import com.example.contentieux_security.dto.DossierCreationRequest;
import com.example.contentieux_security.dto.DossierCreationRequest.RisqueRequest;
import com.example.contentieux_security.dto.DossierCreationRequest.GarantieRequest;
import com.example.contentieux_security.dto.DossierDetailDTO;
import com.example.contentieux_security.dto.GarantieAjoutRequest;
import com.example.contentieux_security.dto.RisqueAjoutRequest;
import com.example.contentieux_security.entity.*;
import com.example.contentieux_security.enums.DossierStatus;
import com.example.contentieux_security.enums.TypeValidateur;
import com.example.contentieux_security.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.contentieux_security.entity.Agence;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DossierService {

    private final DossierRepository       dossierRepository;
    private final AgentBancaireRepository agentRepository;
    private final ClientRepository        clientRepository;
    private final RisqueRepository        risqueRepository;
    private final GarantieRepository      garantieRepository;
    private final HistoriqueService       historiqueService;
    private final NotificationRepository notificationRepository;
    private final NotificationService     notificationService;
    private final ValidateurRepository validateurRepository;

    // ════════════════════════════════════════════════════
    //  LECTURE
    // ════════════════════════════════════════════════════

    public List<DossierContentieux> findAll() {
        return dossierRepository.findAll();
    }

    public DossierContentieux findById(Long id) {
        return dossierRepository.findById(id).orElse(null);
    }

    public DossierContentieux getDossierById(Long id) {
        return dossierRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException(
                        "Dossier introuvable : " + id));
    }

    // ✅ NOUVELLE MÉTHODE : Lecture pour édition avec toutes les relations
    @Transactional(readOnly = true)
    public DossierContentieux getDossierForEdit(Long id) {
        DossierContentieux dossier = dossierRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Dossier introuvable : " + id));
        
        // Forcer le chargement des relations lazy
        if (dossier.getClient() != null) {
            dossier.getClient().getNom();
        }
        if (dossier.getAgence() != null) {
            dossier.getAgence().getNom();
        }
        if (dossier.getRisques() != null) {
            dossier.getRisques().forEach(r -> {
                r.getType();
                if (r.getGaranties() != null) {
                    r.getGaranties().size();
                }
            });
        }
        
        return dossier;
    }

    public List<DossierContentieux> findByClientId(Long clientId) {
        return dossierRepository.findByClient_Id(clientId);
    }

    public List<DossierContentieux> getDossiersAgent(String username) {
        return dossierRepository.findByAgentCreateurUsername(username);
    }

    public DossierContentieux getDossierByIdAndAgent(Long id, String username) {
        DossierContentieux d = getDossierById(id);
        if (d.getAgentCreateur() == null
                || !d.getAgentCreateur().getUsername().equals(username)) {
            throw new RuntimeException("Accès refusé à ce dossier");
        }
        return d;
    }

    @Transactional(readOnly = true)
    public DossierDetailDTO getDossierDetail(Long id) {
        DossierContentieux d = dossierRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Dossier introuvable : " + id));
        return DossierDetailDTO.from(d, historiqueService.getHistorique(id));
    }

    // ════════════════════════════════════════════════════
    //  CRÉATION DOSSIER
    // ════════════════════════════════════════════════════

    @Transactional
    public DossierContentieux creerDossier(DossierCreationRequest request, String agentUsername) {
        AgentBancaire agent = agentRepository.findByUsername(agentUsername)
                .orElseThrow(() -> new RuntimeException("Agent non trouvé"));

        Client client;
        if (request.getClientId() != null) {
            client = clientRepository.findById(request.getClientId())
                    .orElseThrow(() -> new RuntimeException("Client non trouvé"));
        } else {
            client = new Client();
            client.setNom(request.getClientNom());
            client.setPrenom(request.getClientPrenom());
            client.setCin(request.getClientCin());
            client.setEmail(request.getClientEmail());
            client.setTelephone(request.getClientTelephone());
            client.setAdresse(request.getClientAdresse());
            client.setAgence(agent.getAgence());
            client.setDateInscription(LocalDate.now());
            client = clientRepository.save(client);
        }

        DossierContentieux dossier = new DossierContentieux();
        dossier.setNumeroDossier(genererNumeroDossier(agent.getAgence()));
        dossier.setLibelle(request.getLibelle());
        dossier.setDescription(request.getDescription());
        dossier.setNotes(request.getNotes());
        dossier.setClient(client);
        dossier.setAgence(agent.getAgence());
        dossier.setAgentCreateur(agent);
        dossier.setDateCreation(LocalDateTime.now());
        dossier.setStatut(DossierStatus.OUVERT);
        dossier.setCreePar(agentUsername);
        dossier = dossierRepository.save(dossier);

        if (request.getRisques() != null) {
            for (RisqueRequest rq : request.getRisques()) {
                Risque risque = new Risque();
                risque.setType(rq.getType());
                risque.setMontantInitial(rq.getMontantInitial());
                risque.setMontantImpaye(rq.getMontantImpaye());
                if (rq.getDateEcheance() != null && !rq.getDateEcheance().isEmpty())
                    risque.setDateEcheance(LocalDate.parse(rq.getDateEcheance()));
                risque.setDescription(rq.getDescription());
                risque.setDossier(dossier);
                risque = risqueRepository.save(risque);

                if (rq.getGaranties() != null) {
                    for (GarantieRequest gq : rq.getGaranties()) {
                        Garantie garantie = new Garantie();
                        garantie.setTypeGarantie(gq.getTypeGarantie());
                        garantie.setDescription(gq.getDescription());
                        garantie.setValeurEstimee(gq.getValeurEstimee());
                        garantie.setDocumentRef(gq.getDocumentRef());
                        garantie.setRisque(risque);
                        garantieRepository.save(garantie);
                    }
                }
            }
        }

        historiqueService.enregistrer(dossier, HistoriqueService.CREATION,
                "Dossier créé pour " + client.getNom() + " " + client.getPrenom(),
                agentUsername);
        return dossier;
    }

    // ════════════════════════════════════════════════════
    //  WORKFLOW
    // ════════════════════════════════════════════════════

    @Transactional
    public void choisirValidateurs(Long id, String validateurFinancier,
                                    String validateurJuridique, String username) {
        DossierContentieux dossier = getDossierByIdAndAgent(id, username);
        dossier.setValidateurFinancierChoisi(validateurFinancier);
        dossier.setValidateurJuridiqueChoisi(validateurJuridique);
        dossierRepository.save(dossier);
    }

    @Transactional
    public void soumettreAValidation(Long id, String username) {
        DossierContentieux dossier = getDossierByIdAndAgent(id, username);
    
        // Vérifier qu'il y a des risques
        if (dossier.getRisques() == null || dossier.getRisques().isEmpty()) {
            throw new RuntimeException("Ajoutez au moins un risque avant de soumettre.");
        }
    
        // Vérifier qu'au moins un risque est sélectionné
        boolean risqueSelectionne = dossier.getRisques().stream()
                .anyMatch(Risque::isSelectionne);
        if (!risqueSelectionne) {
            throw new RuntimeException("Sélectionnez au moins un crédit à traiter avant de soumettre.");
        }
    
        // Vérifier les validateurs assignés
        if (dossier.getValidateurFinancierChoisi() == null || 
            dossier.getValidateurFinancierChoisi().isBlank()) {
            throw new RuntimeException("Veuillez choisir un validateur financier.");
        }
        if (dossier.getValidateurJuridiqueChoisi() == null || 
            dossier.getValidateurJuridiqueChoisi().isBlank()) {
            throw new RuntimeException("Veuillez choisir un validateur juridique.");
        }
    
        // Changer le statut
        dossier.setStatut(DossierStatus.EN_TRAITEMENT);
        dossier.setValidationFinanciere(null);
        dossier.setValidationJuridique(null);
        dossierRepository.save(dossier);
    
        // Envoyer les notifications
        String messageBase = "Le dossier " + dossier.getNumeroDossier()
                + " de " + dossier.getClient().getNom()
                + " " + dossier.getClient().getPrenom();
    
        notificationService.notifier(
                dossier.getValidateurFinancierChoisi(),
                "Nouveau dossier à valider",
                messageBase + " nécessite votre validation financière.",
                "VALIDATION_FINANCIERE",
                dossier
        );
    
        notificationService.notifier(
                dossier.getValidateurJuridiqueChoisi(),
                "Nouveau dossier à valider",
                messageBase + " nécessite votre validation juridique.",
                "VALIDATION_JURIDIQUE",
                dossier
        );
    
        // Historique
        historiqueService.enregistrer(
                dossier,
                HistoriqueService.SOUMISSION,
                "Soumis à : " + dossier.getValidateurFinancierChoisi()
                        + " (financier) et "
                        + dossier.getValidateurJuridiqueChoisi()
                        + " (juridique)",
                username
        );
    }
    // ════════════════════════════════════════════════════
    //  SÉLECTION/DÉSÉLECTION D'UN RISQUE (CHECKBOX)
    // ════════════════════════════════════════════════════

    /**
     * Sélectionne ou désélectionne un risque spécifique (case à cocher).
     * Permet la sélection multiple (contrairement à l'ancienne méthode).
     */
    @Transactional
    public void selectionnerRisque(Long dossierId, Long risqueId, boolean selectionne, String username) {
        // 1. Vérifier le dossier et les droits
        DossierContentieux dossier = getDossierByIdAndAgent(dossierId, username);
        
        // 2. Vérifier que le dossier est modifiable
        if (dossier.getStatut() != DossierStatus.OUVERT && dossier.getStatut() != DossierStatus.REJETE) {
            throw new RuntimeException("Impossible de modifier la sélection : dossier en cours de traitement");
        }
        
        // 3. Trouver le risque
        Risque risque = risqueRepository.findById(risqueId)
                .orElseThrow(() -> new RuntimeException("Crédit non trouvé"));
        
        // 4. Vérifier que le risque appartient au dossier
        if (!risque.getDossier().getId().equals(dossierId)) {
            throw new RuntimeException("Ce crédit n'appartient pas au dossier");
        }
        
        // 5. Mettre à jour la sélection
        risque.setSelectionne(selectionne);
        risqueRepository.save(risque);
        
        // 6. Historique
        String actionDesc = "Crédit '" + risque.getType() + "' " + 
                           (selectionne ? "sélectionné" : "désélectionné") +
                           " pour la procédure";
        historiqueService.enregistrer(dossier, HistoriqueService.MODIFICATION, actionDesc, username);
    }

    /**
     * Ancienne méthode : sélection unique (radio button style).
     * Garde pour compatibilité si besoin.
     */
    @Transactional
    public void selectionnerRisqueUnique(Long risqueId, String username) {
        Risque risque = risqueRepository.findById(risqueId)
                .orElseThrow(() -> new RuntimeException("Risque introuvable"));

        DossierContentieux dossier = getDossierByIdAndAgent(
                risque.getDossier().getId(), username);

        // Désélectionner tous les autres risques du dossier
        risqueRepository.findByDossier_Id(dossier.getId())
                .forEach(r -> { 
                    if (!r.getId().equals(risqueId)) {
                        r.setSelectionne(false); 
                        risqueRepository.save(r); 
                    }
                });

        // Sélectionner celui-ci
        risque.setSelectionne(true);
        risqueRepository.save(risque);

        historiqueService.enregistrer(dossier, HistoriqueService.SELECTION_RISQUE,
                "Crédit sélectionné : " + risque.getType(), username);
    }

    // ════════════════════════════════════════════════════
    //  GARANTIES
    // ════════════════════════════════════════════════════

    @Transactional
    public Garantie ajouterGarantie(Long risqueId, GarantieAjoutRequest request,
                                     String username) {
        Risque risque = risqueRepository.findById(risqueId)
                .orElseThrow(() -> new RuntimeException("Risque introuvable"));

        getDossierByIdAndAgent(risque.getDossier().getId(), username);

        Garantie garantie = new Garantie();
        garantie.setTypeGarantie(request.getTypeGarantie());
        garantie.setDescription(request.getDescription());
        garantie.setValeurEstimee(request.getValeurEstimee());
        garantie.setDocumentRef(request.getDocumentRef());
        garantie.setRisque(risque);
        garantieRepository.save(garantie);

        historiqueService.enregistrer(risque.getDossier(), HistoriqueService.AJOUT_GARANTIE,
                "Garantie ajoutée : " + request.getTypeGarantie(), username);
        return garantie;
    }

    @Transactional
    public void modifierGarantie(Long garantieId, String typeGarantie,
                                  String description, Double valeurEstimee,
                                  String documentRef, String username) {
        Garantie g = garantieRepository.findByIdWithRisqueAndDossier(garantieId)
                .orElseThrow(() -> new RuntimeException("Garantie introuvable"));

        getDossierByIdAndAgent(g.getRisque().getDossier().getId(), username);

        g.setTypeGarantie(typeGarantie);
        g.setDescription(description);
        g.setValeurEstimee(valeurEstimee);
        g.setDocumentRef(documentRef);
        garantieRepository.save(g);
    }

    // ════════════════════════════════════════════════════
    //  VALIDATION
    // ════════════════════════════════════════════════════

    @Transactional(readOnly = true)
public List<DossierContentieux> getDossiersEnAttenteValidationFinanciere(String username) {
    List<DossierContentieux> dossiers = dossierRepository.findEnAttenteValidationFinanciereParValidateur(username);
    
    // Forcer le chargement des relations lazy
    for (DossierContentieux d : dossiers) {
        if (d.getClient() != null) {
            d.getClient().getNom(); // Force le chargement
        }
        if (d.getAgence() != null) {
            d.getAgence().getNom(); // Force le chargement
        }
    }
    return dossiers;
}
@Transactional(readOnly = true)
    public List<DossierContentieux> getDossiersEnAttenteValidationJuridique(String username) {
        return dossierRepository.findEnAttenteValidationJuridiqueParValidateur(username);
    }
    @Transactional(readOnly = true)
    public List<DossierContentieux> getDossiersByStatut(DossierStatus statut) {
        return dossierRepository.findByStatut(statut);
    }

    // ════════════════════════════════════════════════════
    //  UTILITAIRE
    // ════════════════════════════════════════════════════

    private String genererNumeroDossier(Agence agence) {
        if (agence == null || agence.getCode() == null)
            throw new RuntimeException("Agence ou code agence null");

        String prefix = "DOS-" + agence.getCode() + "-" + LocalDate.now().getYear();
        Optional<String> lastNumero = dossierRepository.findLastNumero(prefix);

        int sequence = 1;
        if (lastNumero.isPresent()) {
            try {
                String[] parts = lastNumero.get().split("-");
                sequence = Integer.parseInt(parts[parts.length - 1]) + 1;
            } catch (Exception ignored) { }
        }
        return String.format("%s-%05d", prefix, sequence);
    }

    @Transactional
    public void supprimerDossier(Long id, String username) {
        DossierContentieux d = getDossierByIdAndAgent(id, username);
    
        garantieRepository.deleteByDossierId(id);
        risqueRepository.deleteByDossierId(id);
        historiqueService.supprimerParDossier(id);
        notificationRepository.deleteByDossierId(id);
        dossierRepository.deleteById(id);
    }

    public void ajouterRisque(Long id, RisqueAjoutRequest request, String username) {
        throw new UnsupportedOperationException("Unimplemented method 'ajouterRisque'");
    }

    // ════════════════════════════════════════════════════
    //  MODIFICATION DOSSIER
    // ════════════════════════════════════════════════════

    @Transactional
    public DossierContentieux modifierDossier(Long id, DossierCreationRequest request,
                                               String agentUsername) {
        DossierContentieux dossier = getDossierByIdAndAgent(id, agentUsername);

        if (dossier.getStatut() != DossierStatus.OUVERT
                && dossier.getStatut() != DossierStatus.REJETE) {
            throw new RuntimeException(
                "Ce dossier ne peut plus être modifié (statut : " + dossier.getStatut() + ")");
        }

        if (request.getLibelle() != null)     dossier.setLibelle(request.getLibelle());
        if (request.getDescription() != null) dossier.setDescription(request.getDescription());
        if (request.getNotes() != null)        dossier.setNotes(request.getNotes());

        if (dossier.getStatut() == DossierStatus.REJETE) {
            dossier.setStatut(DossierStatus.OUVERT);
            if (Boolean.FALSE.equals(dossier.getValidationFinanciere())) {
                dossier.setValidationFinanciere(null);
                dossier.setCommentaireFinancier(null);
            }
            if (Boolean.FALSE.equals(dossier.getValidationJuridique())) {
                dossier.setValidationJuridique(null);
                dossier.setCommentaireJuridique(null);
            }
        }

        dossier = dossierRepository.save(dossier);

        historiqueService.enregistrer(dossier, "MODIFICATION",
                "Dossier modifié", agentUsername);

        return dossier;
    }



    @Transactional
    public void validerFinancier(Long dossierId, String username, boolean accepte, String commentaire) {
    
        // 🔹 Charger le dossier AVEC l’agent (important pour éviter LazyInitializationException)
        DossierContentieux dossier = dossierRepository.findByIdWithDetails(dossierId)
        .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));
        // 🔹 Forcer le chargement de l'agent (solution simple si pas de JOIN FETCH)
        if (dossier.getAgentCreateur() != null) {
            dossier.getAgentCreateur().getUsername();
        }
    
        // 🔹 Vérifier que c'est bien le validateur assigné
        if (!username.equals(dossier.getValidateurFinancierChoisi())) {
            throw new RuntimeException("Vous n'êtes pas le validateur assigné");
        }
    
        // 🔹 Vérifier statut
        if (dossier.getStatut() != DossierStatus.EN_TRAITEMENT) {
            throw new RuntimeException("Ce dossier n'est pas en attente de validation");
        }
    
        // 🔹 Mise à jour
        dossier.setValidationFinanciere(accepte);
        dossier.setCommentaireFinancier(commentaire);
        dossier.setValidateurFinancierUsername(username);
    
        // 🔹 Gestion des statuts
        if (!accepte) {
            dossier.setStatut(DossierStatus.REJETE);
        } else if (Boolean.TRUE.equals(dossier.getValidationJuridique())) {
            dossier.setStatut(DossierStatus.VALIDE);
        }
    
        dossierRepository.save(dossier);
    
        // 🔹 Sécuriser username de l’agent
        String agentUsername = (dossier.getAgentCreateur() != null)
                ? dossier.getAgentCreateur().getUsername()
                : null;
    
        // 🔹 Message
        String message = accepte
                ? "Votre dossier " + dossier.getNumeroDossier() + " a été validé financièrement"
                : "Votre dossier " + dossier.getNumeroDossier() + " a été rejeté financièrement";
    
        // 🔹 Notification
        if (agentUsername != null) {
            notificationService.notifier(
                    agentUsername,
                    accepte ? "Validation financière acceptée" : "Validation financière rejetée",
                    message + (commentaire != null ? ". Commentaire: " + commentaire : ""),
                    accepte ? "VALIDATION_OK" : "VALIDATION_KO",
                    dossier
            );
        }
    
        // 🔹 Historique
        historiqueService.enregistrer(
                dossier,
                accepte ? HistoriqueService.VALIDATION_FIN : HistoriqueService.REJET_FIN,
                accepte
                        ? "Validé par " + username
                        : "Rejeté par " + username + ". Motif: " + commentaire,
                username
        );
    }

}