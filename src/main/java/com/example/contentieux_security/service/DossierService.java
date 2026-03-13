package com.example.contentieux_security.service;

import com.example.contentieux_security.dto.DossierCreationRequest;
import com.example.contentieux_security.dto.DossierCreationRequest.RisqueRequest;
import com.example.contentieux_security.dto.DossierCreationRequest.GarantieRequest;
import com.example.contentieux_security.dto.DossierDetailDTO;
import com.example.contentieux_security.dto.GarantieAjoutRequest;
import com.example.contentieux_security.dto.RisqueAjoutRequest;
import com.example.contentieux_security.entity.*;
import com.example.contentieux_security.enums.DossierStatus;
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

    // ════════════════════════════════════════════════════
    //  LECTURE
    // ════════════════════════════════════════════════════

    public List<DossierContentieux> findAll() {
        return dossierRepository.findAll();
    }

    public DossierContentieux findById(Long id) {
        return dossierRepository.findById(id).orElse(null);
    }

    /** Lève une exception si non trouvé (utilisé par le Controller) */
    public DossierContentieux getDossierById(Long id) {
        return dossierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dossier introuvable : " + id));
    }

    public List<DossierContentieux> findByClientId(Long clientId) {
        return dossierRepository.findByClient_Id(clientId);
    }

    public List<DossierContentieux> getDossiersAgent(String username) {
        return dossierRepository.findByAgentCreateur_Username(username);
    }

    public DossierContentieux getDossierByIdAndAgent(Long id, String username) {
        DossierContentieux d = getDossierById(id);
        if (d.getAgentCreateur() == null
                || !d.getAgentCreateur().getUsername().equals(username)) {
            throw new RuntimeException("Accès refusé à ce dossier");
        }
        return d;
    }

    /** DTO complet (évite les LazyInitializationException dans Thymeleaf) */
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

        // ── Client ─────────────────────────────────────
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

        // ── Dossier ────────────────────────────────────
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

        // ── Risques & Garanties ────────────────────────
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
    public void soumettreAValidation(Long id, String username) {
        DossierContentieux dossier = getDossierByIdAndAgent(id, username);

        if (dossier.getRisques() == null || dossier.getRisques().isEmpty())
            throw new RuntimeException("Ajoutez au moins un risque avant de soumettre.");

        if (dossier.getRisques().stream().noneMatch(Risque::isSelectionne))
            throw new RuntimeException("Sélectionnez le risque à traiter avant de soumettre.");

        dossier.setStatut(DossierStatus.EN_TRAITEMENT);
        dossierRepository.save(dossier);

        historiqueService.enregistrer(dossier, HistoriqueService.SOUMISSION,
                "Soumis à validation financière et juridique", username);
    }

    // ════════════════════════════════════════════════════
    //  RISQUES
    // ════════════════════════════════════════════════════

    @Transactional
    public Risque ajouterRisque(Long dossierId, RisqueAjoutRequest request, String username) {
        DossierContentieux dossier = getDossierByIdAndAgent(dossierId, username);

        Risque risque = new Risque();
        risque.setType(request.getType());
        risque.setMontantInitial(request.getMontantInitial());
        risque.setMontantImpaye(request.getMontantImpaye());
        risque.setDescription(request.getDescription());
        risque.setDossier(dossier);
        if (request.getDateEcheance() != null && !request.getDateEcheance().isBlank())
            risque.setDateEcheance(LocalDate.parse(request.getDateEcheance()));
        risqueRepository.save(risque);

        historiqueService.enregistrer(dossier, HistoriqueService.AJOUT_RISQUE,
                "Crédit ajouté : " + request.getType(), username);
        return risque;
    }

    @Transactional
    public void selectionnerRisque(Long risqueId, String username) {
        Risque risque = risqueRepository.findById(risqueId)
                .orElseThrow(() -> new RuntimeException("Risque introuvable"));

        DossierContentieux dossier = getDossierByIdAndAgent(risque.getDossier().getId(), username);

        // Désélectionner tous, puis sélectionner le demandé
        risqueRepository.findByDossier_Id(dossier.getId())
                .forEach(r -> { r.setSelectionne(false); risqueRepository.save(r); });

        risque.setSelectionne(true);
        risqueRepository.save(risque);

        historiqueService.enregistrer(dossier, HistoriqueService.SELECTION_RISQUE,
                "Crédit sélectionné : " + risque.getType(), username);
    }

    // ════════════════════════════════════════════════════
    //  GARANTIES
    // ════════════════════════════════════════════════════

    @Transactional
    public Garantie ajouterGarantie(Long risqueId, GarantieAjoutRequest request, String username) {
        Risque risque = risqueRepository.findById(risqueId)
                .orElseThrow(() -> new RuntimeException("Risque introuvable"));

        getDossierByIdAndAgent(risque.getDossier().getId(), username); // vérifie accès

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

    // ════════════════════════════════════════════════════
    //  VALIDATION (utilisé par le Controller validateur)
    // ════════════════════════════════════════════════════

    public List<DossierContentieux> getDossiersEnAttenteValidationFinanciere() {
        return dossierRepository.findEnAttenteValidationFinanciere();
    }

    public List<DossierContentieux> getDossiersEnAttenteValidationJuridique() {
        return dossierRepository.findEnAttenteValidationJuridique();
    }

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
}