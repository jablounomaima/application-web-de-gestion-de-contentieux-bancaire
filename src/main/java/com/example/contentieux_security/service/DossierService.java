package com.example.contentieux_security.service;

import com.example.contentieux_security.dto.DossierCreationRequest;
import com.example.contentieux_security.dto.DossierCreationRequest.RisqueRequest;
import com.example.contentieux_security.dto.DossierCreationRequest.GarantieRequest;
import com.example.contentieux_security.dto.DossierDetailDTO;
import com.example.contentieux_security.dto.GarantieAjoutRequest;
import com.example.contentieux_security.dto.RisqueAjoutRequest;
import com.example.contentieux_security.entity.*;
import com.example.contentieux_security.enums.DossierStatus;
import com.example.contentieux_security.enums.TypeClient;
import com.example.contentieux_security.enums.TypeValidateur;
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
public class DossierService {

    private final DossierRepository       dossierRepository;
    private final AgentBancaireRepository agentRepository;
    private final ClientRepository        clientRepository;
    private final RisqueRepository        risqueRepository;
    private final GarantieRepository      garantieRepository;
    private final HistoriqueService       historiqueService;
    private final NotificationRepository  notificationRepository;
    private final NotificationService     notificationService;
    private final ValidateurRepository    validateurRepository;

    // ════════════════════════════════════════════════════
    //  LECTURE
    // ════════════════════════════════════════════════════

    public List<DossierContentieux> findAll() {
        return dossierRepository.findAll();
    }

    public DossierContentieux findById(Long id) {
        return dossierRepository.findById(id).orElse(null);
    }

   // DossierService.java
public DossierContentieux getDossierById(Long id) {
    return dossierRepository.findByIdWithDetails(id)
            .orElseThrow(() -> new IllegalArgumentException("Dossier introuvable : id=" + id));
}

    @Transactional(readOnly = true)
    public DossierContentieux getDossierForEdit(Long id) {
        DossierContentieux dossier = dossierRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Dossier introuvable : " + id));

        if (dossier.getClient() != null) dossier.getClient().getNom();
        if (dossier.getAgence() != null) dossier.getAgence().getNom();
        if (dossier.getRisques() != null) {
            dossier.getRisques().forEach(r -> {
                r.getType();
                if (r.getGaranties() != null) r.getGaranties().size();
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
// ❌ SUPPRIMER CE BLOC ENTIER (lignes ~88 à ~111)
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

            // 🔥 Type client avec valeur par défaut
            TypeClient typeClient = request.getTypeClient() != null
                    ? request.getTypeClient()
                    : TypeClient.PARTICULIER;

            client.setTypeClient(typeClient);
            client.setNom(clean(request.getClientNom()));
            client.setPrenom(clean(request.getClientPrenom()));
            client.setEmail(clean(request.getClientEmail()));
            client.setTelephone(clean(request.getClientTelephone()));
            client.setAdresse(clean(request.getClientAdresse()));
            client.setAgence(agent.getAgence());
            client.setDateInscription(LocalDate.now());

            // 🔥 Cohérence métier selon le type
            if (typeClient == TypeClient.ENTREPRISE) {
                client.setCin(null);
                client.setPrenom(null);
                client.setRne(clean(request.getClientRne()));
                client.setRaisonSociale(clean(request.getClientRaisonSociale()));
            } else {
                client.setPrenom(clean(request.getClientPrenom()));
                client.setCin(clean(request.getClientCin()));
                client.setRne(null);
                client.setRaisonSociale(null);
            }

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
                "Dossier créé pour " + client.getNom(), agentUsername);

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
         
    Client client = dossier.getClient();
    client.getNom();
    client.getPrenom();
    client.getCin();
    client.getRne();
    client.getRaisonSociale();

        if (dossier.getRisques() == null || dossier.getRisques().isEmpty())
            throw new RuntimeException("Ajoutez au moins un risque avant de soumettre.");

        boolean risqueSelectionne = dossier.getRisques().stream().anyMatch(Risque::isSelectionne);
        if (!risqueSelectionne)
            throw new RuntimeException("Sélectionnez au moins un crédit à traiter avant de soumettre.");

        if (dossier.getValidateurFinancierChoisi() == null ||
                dossier.getValidateurFinancierChoisi().isBlank())
            throw new RuntimeException("Veuillez choisir un validateur financier.");

        if (dossier.getValidateurJuridiqueChoisi() == null ||
                dossier.getValidateurJuridiqueChoisi().isBlank())
            throw new RuntimeException("Veuillez choisir un validateur juridique.");

        dossier.setStatut(DossierStatus.EN_TRAITEMENT);
        dossier.setValidationFinanciere(null);
        dossier.setValidationJuridique(null);
        dossierRepository.save(dossier);

        String messageBase = "Le dossier " + dossier.getNumeroDossier()
                + " de " + dossier.getClient().getNom()
                + " " + (dossier.getClient().getPrenom() != null ? dossier.getClient().getPrenom() : "");

        notificationService.notifier(dossier.getValidateurFinancierChoisi(),
                "Nouveau dossier à valider",
                messageBase + " nécessite votre validation financière.",
                "VALIDATION_FINANCIERE", dossier);

        notificationService.notifier(dossier.getValidateurJuridiqueChoisi(),
                "Nouveau dossier à valider",
                messageBase + " nécessite votre validation juridique.",
                "VALIDATION_JURIDIQUE", dossier);

        historiqueService.enregistrer(dossier, HistoriqueService.SOUMISSION,
                "Soumis à : " + dossier.getValidateurFinancierChoisi()
                        + " (financier) et " + dossier.getValidateurJuridiqueChoisi()
                        + " (juridique)", username);
    }

    // ════════════════════════════════════════════════════
    //  SÉLECTION RISQUE
    // ════════════════════════════════════════════════════

    @Transactional
    public void selectionnerRisque(Long dossierId, Long risqueId,
                                    boolean selectionne, String username) {
        DossierContentieux dossier = getDossierByIdAndAgent(dossierId, username);

        if (dossier.getStatut() != DossierStatus.OUVERT
                && dossier.getStatut() != DossierStatus.REJETE)
            throw new RuntimeException("Impossible de modifier la sélection : dossier en cours de traitement");

        Risque risque = risqueRepository.findById(risqueId)
                .orElseThrow(() -> new RuntimeException("Crédit non trouvé"));

        if (!risque.getDossier().getId().equals(dossierId))
            throw new RuntimeException("Ce crédit n'appartient pas au dossier");

        risque.setSelectionne(selectionne);
        risqueRepository.save(risque);

        historiqueService.enregistrer(dossier, HistoriqueService.MODIFICATION,
                "Crédit '" + risque.getType() + "' "
                        + (selectionne ? "sélectionné" : "désélectionné")
                        + " pour la procédure", username);
    }

    @Transactional
    public void selectionnerRisqueUnique(Long risqueId, String username) {
        Risque risque = risqueRepository.findById(risqueId)
                .orElseThrow(() -> new RuntimeException("Risque introuvable"));

        DossierContentieux dossier = getDossierByIdAndAgent(
                risque.getDossier().getId(), username);

        risqueRepository.findByDossier_Id(dossier.getId()).forEach(r -> {
            if (!r.getId().equals(risqueId)) {
                r.setSelectionne(false);
                risqueRepository.save(r);
            }
        });

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
        List<DossierContentieux> dossiers =
                dossierRepository.findEnAttenteValidationFinanciereParValidateur(username);
        for (DossierContentieux d : dossiers) {
            if (d.getClient() != null) d.getClient().getNom();
            if (d.getAgence() != null) d.getAgence().getNom();
        }
        return dossiers;
    }

    @Transactional(readOnly = true)
    public List<DossierContentieux> getDossiersEnAttenteValidationJuridique(String username) {
        List<DossierContentieux> dossiers =
                dossierRepository.findEnAttenteValidationJuridiqueParValidateur(username);
        for (DossierContentieux d : dossiers) {
            if (d.getClient() != null) d.getClient().getNom();
            if (d.getAgence() != null) d.getAgence().getNom();
            if (d.getAgentCreateur() != null) d.getAgentCreateur().getUsername();
        }
        return dossiers;
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

    // 🔥 Méthode clean centralisée
    private String clean(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Transactional
    public void supprimerDossier(Long id, String username) {
        DossierContentieux dossier = dossierRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Dossier introuvable"));

        for (Risque r : dossier.getRisques()) {
            for (Garantie g : r.getGaranties()) g.setRisque(null);
            r.getGaranties().clear();
        }
        dossier.getRisques().clear();
        dossierRepository.delete(dossier);
    }

    @Transactional
    public void ajouterRisque(Long dossierId, RisqueAjoutRequest request, String username) {
        DossierContentieux dossier = getDossierByIdAndAgent(dossierId, username);

        if (dossier.getStatut() != DossierStatus.OUVERT
                && dossier.getStatut() != DossierStatus.REJETE)
            throw new RuntimeException("Impossible d'ajouter un risque : dossier en cours de traitement");

        Risque risque = new Risque();
        risque.setType(request.getType());
        risque.setMontantInitial(request.getMontantInitial());
        risque.setMontantImpaye(request.getMontantImpaye());
        risque.setDescription(request.getDescription());

        if (request.getDateEcheance() != null && !request.getDateEcheance().isEmpty())
            risque.setDateEcheance(LocalDate.parse(request.getDateEcheance()));

        risque.setDossier(dossier);
        risqueRepository.save(risque);

        historiqueService.enregistrer(dossier, HistoriqueService.AJOUT_RISQUE,
                "Ajout d'un crédit : " + request.getType(), username);
    }

    // ════════════════════════════════════════════════════
    //  MODIFICATION DOSSIER
    // ════════════════════════════════════════════════════

    @Transactional
    public DossierContentieux modifierDossier(Long id, DossierCreationRequest request,
                                               String agentUsername) {
        DossierContentieux dossier = getDossierByIdAndAgent(id, agentUsername);

        if (dossier.getStatut() != DossierStatus.OUVERT
                && dossier.getStatut() != DossierStatus.REJETE)
            throw new RuntimeException(
                    "Ce dossier ne peut plus être modifié (statut : " + dossier.getStatut() + ")");

        if (request.getLibelle() != null)     dossier.setLibelle(request.getLibelle());
        if (request.getDescription() != null) dossier.setDescription(request.getDescription());
        if (request.getNotes() != null)       dossier.setNotes(request.getNotes());

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
        historiqueService.enregistrer(dossier, "MODIFICATION", "Dossier modifié", agentUsername);
        return dossier;
    }

    @Transactional
    public void validerFinancier(Long dossierId, String username,
                                  boolean accepte, String commentaire) {
        DossierContentieux dossier = dossierRepository.findByIdWithDetails(dossierId)
                .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

        if (dossier.getAgentCreateur() != null)
            dossier.getAgentCreateur().getUsername();

        if (!username.equals(dossier.getValidateurFinancierChoisi()))
            throw new RuntimeException("Vous n'êtes pas le validateur assigné");

        if (dossier.getStatut() != DossierStatus.EN_TRAITEMENT)
            throw new RuntimeException("Ce dossier n'est pas en attente de validation");

        dossier.setValidationFinanciere(accepte);
        dossier.setCommentaireFinancier(commentaire);
        dossier.setValidateurFinancierUsername(username);

        if (!accepte) {
            dossier.setStatut(DossierStatus.REJETE);
        } else if (Boolean.TRUE.equals(dossier.getValidationJuridique())) {
            dossier.setStatut(DossierStatus.VALIDE);
        }

        dossierRepository.save(dossier);

        String agentUsername = dossier.getAgentCreateur() != null
                ? dossier.getAgentCreateur().getUsername() : null;

        String message = accepte
                ? "Votre dossier " + dossier.getNumeroDossier() + " a été validé financièrement"
                : "Votre dossier " + dossier.getNumeroDossier() + " a été rejeté financièrement";

        if (agentUsername != null) {
            notificationService.notifier(agentUsername,
                    accepte ? "Validation financière acceptée" : "Validation financière rejetée",
                    message + (commentaire != null ? ". Commentaire: " + commentaire : ""),
                    accepte ? "VALIDATION_OK" : "VALIDATION_KO", dossier);
        }

        historiqueService.enregistrer(dossier,
                accepte ? HistoriqueService.VALIDATION_FIN : HistoriqueService.REJET_FIN,
                accepte ? "Validé par " + username
                        : "Rejeté par " + username + ". Motif: " + commentaire,
                username);
    }

    // ════════════════════════════════════════════════════
    //  RECHERCHE
    // ════════════════════════════════════════════════════

    public List<DossierContentieux> rechercherDossiers(String username, String keyword) {
        if (keyword == null || keyword.trim().isEmpty())
            return getDossiersAgent(username);
        return dossierRepository.rechercherParAgent(username, keyword.trim());
    }






}