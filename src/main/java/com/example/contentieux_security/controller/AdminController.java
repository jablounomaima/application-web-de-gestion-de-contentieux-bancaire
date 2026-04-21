package com.example.contentieux_security.controller;

import com.example.contentieux_security.dto.*;
import com.example.contentieux_security.enums.TypeValidateur;
import com.example.contentieux_security.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.example.contentieux_security.dto.ValidateurDTO;
import java.util.List;

/**
 * Contrôleur d'administration pour la gestion des ressources du système.
 * Toutes les routes nécessitent le rôle ADMIN.
 * Gère : Agences, Agents Bancaires, Validateurs
 */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")  // Sécurité : seuls les admins peuvent accéder
@RequiredArgsConstructor  // Injection automatique des dépendances finales
public class AdminController {

    // ═══════════════════════════════════════════════════════════════════════
    // SERVICES INJECTÉS
    // ═══════════════════════════════════════════════════════════════════════
    
    /** Service de gestion des agences bancaires */
    private final AgenceService agenceService;
    
    /** Service de gestion des agents bancaires (création dans Keycloak + BDD) */
    private final AgentBancaireService agentService;
    
    /** Service de gestion des validateurs (financiers et juridiques) */
    private final ValidateurService validateurService;

    // ═══════════════════════════════════════════════════════════════════════
    // DASHBOARD ADMIN
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Affiche le tableau de bord de l'administrateur.
     * Récupère le nom complet de l'admin depuis le token OIDC de Keycloak.
     * 
     * @param authentication Objet Spring Security contenant l'utilisateur authentifié
     * @param model Modèle Thymeleaf pour passer des données à la vue
     * @return Nom du template Thymeleaf à afficher
     */
    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        String fullname = "";

        // Extraction des informations utilisateur depuis le token OIDC Keycloak
        if (authentication.getPrincipal() instanceof
                org.springframework.security.oauth2.core.oidc.user.OidcUser oidc) {

            String given = oidc.getAttribute("given_name");   // Prénom
            String family = oidc.getAttribute("family_name"); // Nom

            if (given != null) fullname += given + " ";
            if (family != null) fullname += family;
        }

        // Passage des données au template
        model.addAttribute("fullname", fullname.trim());
        model.addAttribute("nouveauUtilisateur", new UtilisateurDto());

        return "admin/dashboard";
    }

    // ═══════════════════════════════════════════════════════════════════════
    // GESTION DES AGENCES
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Liste toutes les agences bancaires.
     * Affiche le formulaire de création d'une nouvelle agence.
     * 
     * @param model Modèle pour la vue
     * @return Template de liste des agences
     */
    @GetMapping("/agences")
    public String listAgences(Model model) {
        // Récupération de toutes les agences pour affichage
        model.addAttribute("agences", agenceService.getAllAgences());
        // Objet vide pour le formulaire de création
        model.addAttribute("agence", new AgenceDTO());

        return "admin/agences";
    }

    /**
     * Crée une nouvelle agence bancaire.
     * Redirige vers la liste avec message de succès ou erreur.
     * 
     * @param dto Données de l'agence à créer (depuis le formulaire)
     * @param ra RedirectAttributes pour messages flash (survie après redirection)
     * @return Redirection vers la liste des agences
     */
    @PostMapping("/agences")
    public String createAgence(@ModelAttribute AgenceDTO dto,
                               RedirectAttributes ra) {
        try {
            agenceService.createAgence(dto);
            ra.addFlashAttribute("success", "Agence créée avec succès !");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Erreur création agence: " + e.getMessage());
        }
        return "redirect:/admin/agences";
    }

    /**
     * Met à jour une agence existante.
     * 
     * @param id Identifiant de l'agence à modifier
     * @param dto Nouvelles données de l'agence
     * @param ra Messages flash pour la redirection
     * @return Redirection vers la liste
     */
    @PostMapping("/agences/{id}/update")
    public String updateAgence(@PathVariable Long id,
                               @ModelAttribute AgenceDTO dto,
                               RedirectAttributes ra) {
        try {
            agenceService.updateAgence(id, dto);
            ra.addFlashAttribute("success", "Agence mise à jour !");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Erreur mise à jour: " + e.getMessage());
        }
        return "redirect:/admin/agences";
    }

    /**
     * Supprime une agence bancaire.
     * ⚠️ Vérifier qu'aucun agent n'est rattaché avant suppression.
     * 
     * @param id Identifiant de l'agence à supprimer
     * @param ra Messages flash
     * @return Redirection vers la liste
     */
    @PostMapping("/agences/{id}/delete")
    public String deleteAgence(@PathVariable Long id,
                               RedirectAttributes ra) {
        try {
            agenceService.deleteAgence(id);
            ra.addFlashAttribute("success", "Agence supprimée !");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Erreur suppression: " + e.getMessage());
        }
        return "redirect:/admin/agences";
    }

    // ═══════════════════════════════════════════════════════════════════════
    // GESTION DES AGENTS BANCAIRES
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Liste tous les agents bancaires.
     * Affiche le formulaire de création avec la liste des agences disponibles.
     * 
     * @param model Modèle pour la vue
     * @return Template de gestion des agents
     */
    @GetMapping("/agents")
    public String listAgents(Model model) {
        // Liste de tous les agents (DTO pour éviter les références circulaires)
        model.addAttribute("agents", agentService.getAllAgents());
        // Liste des agences pour le select du formulaire
        model.addAttribute("agences", agenceService.getAllAgences());
        // Objet vide pour le formulaire de création
        model.addAttribute("agent", new AgentCreationRequest());

        return "admin/agents";
    }

    /**
     * Crée un nouvel agent bancaire.
     * 🔥 POINT CRITIQUE : Cette méthode appelle agentService.createAgent() qui :
     *   1. Crée l'utilisateur dans Keycloak (authentification)
     *   2. Crée l'agent dans la base de données locale (métier)
     *   3. Assigne une agence obligatoire
     * 
     * Si Keycloak échoue → pas de création en base (transaction)
     * Si la base échoue → suppression compensatoire dans Keycloak
     * 
     * @param request Données de création (username, password, agenceId, etc.)
     * @param ra Messages flash pour retour utilisateur
     * @return Redirection vers la liste des agents
     */
    @PostMapping("/agents")
    public String createAgent(@ModelAttribute AgentCreationRequest request,
                              RedirectAttributes ra) {
        try {
            // Appel au service qui gère la synchronisation Keycloak + BDD
            agentService.createAgent(request);
            ra.addFlashAttribute("success", 
                "Agent '" + request.getUsername() + "' créé avec succès !");
        } catch (Exception e) {
            // Affichage du message d'erreur détaillé (Keycloak ou BDD)
            ra.addFlashAttribute("error", "Erreur création agent: " + e.getMessage());
        }
        return "redirect:/admin/agents";
    }

    @GetMapping("/agents/{id}/edit")
public String editAgentForm(@PathVariable Long id, Model model) {
    model.addAttribute("agent",   agentService.getAgentById(id));
    model.addAttribute("agences", agenceService.getAllAgences());
    return "admin/agent-edit";
}

    /**
     * Met à jour un agent bancaire existant.
     * Modifie uniquement les données métier (pas le mot de passe dans Keycloak ici).
     * 
     * @param id Identifiant de l'agent
     * @param request Nouvelles données
     * @param ra Messages flash
     * @return Redirection vers la liste
     */
    @PostMapping("/agents/{id}/update")
    public String updateAgent(@PathVariable Long id,
                              @ModelAttribute AgentCreationRequest request,
                              RedirectAttributes ra) {
        try {
            agentService.updateAgent(id, request);
            ra.addFlashAttribute("success", "Agent mis à jour !");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Erreur mise à jour: " + e.getMessage());
        }
        return "redirect:/admin/agents";
    }

    /**
     * Supprime définitivement un agent.
     * Supprime dans Keycloak ET dans la base de données.
     * 
     * @param id Identifiant de l'agent à supprimer
     * @param ra Messages flash
     * @return Redirection vers la liste
     */
    @PostMapping("/agents/{id}/delete")
    public String deleteAgent(@PathVariable Long id,
                              RedirectAttributes ra) {
        try {
            agentService.deleteAgent(id);
            ra.addFlashAttribute("success", "Agent supprimé !");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Erreur suppression: " + e.getMessage());
        }
        return "redirect:/admin/agents";
    }

    /**
     * Active/Désactive un agent (soft delete).
     * Change le statut 'actif' sans supprimer les données.
     * 
     * @param id Identifiant de l'agent
     * @param ra Messages flash
     * @return Redirection vers la liste
     */
    @PostMapping("/agents/{id}/toggle")
    public String toggleAgent(@PathVariable Long id,
                              RedirectAttributes ra) {
        try {
            agentService.toggleAgentStatus(id);
            ra.addFlashAttribute("success", "Statut modifié !");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/agents";
    }

    // ═══════════════════════════════════════════════════════════════════════
    // GESTION DES VALIDATEURS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Liste tous les validateurs par catégorie.
     * Sépare les validateurs financiers et juridiques pour l'affichage.
     * 
     * @param model Modèle pour la vue
     * @return Template de gestion des validateurs
     */
    @GetMapping("/validateurs")
    public String listValidateurs(Model model) {
        // Validateurs financiers actifs
        model.addAttribute("validateursFinanciers",
                validateurService.getByTypeAndActif(TypeValidateur.VALIDATEUR_FINANCIER));

        // Validateurs juridiques actifs
        model.addAttribute("validateursJuridiques",
                validateurService.getByTypeAndActif(TypeValidateur.VALIDATEUR_JURIDIQUE));

        // Données pour le formulaire de création
        model.addAttribute("agences", agenceService.getAllAgences());
        model.addAttribute("validateur", new ValidateurCreationRequest());

        return "admin/validateurs";
    }

    /**
     * Affiche le formulaire de création d'un validateur.
     * Charge les listes déroulantes (agences et types).
     * 
     * @param model Modèle pour la vue
     * @return Template du formulaire
     */
    @GetMapping("/validateurs/new")
    public String form(Model model) {
        model.addAttribute("validateur", new ValidateurCreationRequest());
        model.addAttribute("agences", agenceService.getAllAgences());
        model.addAttribute("types", TypeValidateur.values()); // Enum: FINANCIER, JURIDIQUE
        return "admin/validateur-form";
    }

    /**
     * Crée un nouveau validateur.
     * Les validateurs sont des utilisateurs spéciaux pour la validation des dossiers.
     * 
     * @param request Données du validateur à créer
     * @param ra Messages flash
     * @return Redirection vers la liste
     */
    @PostMapping("/validateurs")
    public String create(@ModelAttribute ValidateurCreationRequest request,
                         RedirectAttributes ra) {
        try {
            validateurService.creerValidateur(request);
            ra.addFlashAttribute("success", "Validateur créé avec succès !");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Erreur création validateur: " + e.getMessage());
        }
        return "redirect:/admin/validateurs";
    }

    /**
     * Active/Désactive un validateur.
     * 
     * @param id Identifiant du validateur
     * @param ra Messages flash
     * @return Redirection vers la liste des validateurs
     */
    @PostMapping("/validateurs/{id}/toggle")  // ✅ CORRIGÉ: chemin complet pour éviter conflit
    public String toggleValidateur(@PathVariable Long id,
                                   RedirectAttributes ra) {
        try {
            validateurService.toggleActif(id);
            ra.addFlashAttribute("success", "Statut du validateur modifié !");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/validateurs";
    }

    /**
     * Supprime définitivement un validateur.
     * 
     * @param id Identifiant du validateur à supprimer
     * @param ra Messages flash
     * @return Redirection vers la liste
     */
    @PostMapping("/validateurs/{id}/delete")  // ✅ CORRIGÉ: chemin complet pour éviter conflit
    public String deleteValidateur(@PathVariable Long id,
                                   RedirectAttributes ra) {
        try {
            validateurService.delete(id);
            ra.addFlashAttribute("success", "Validateur supprimé !");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/validateurs";
    }

// ═══════════════════════════════════════════════════════════════════════
// GESTION DES VALIDATEURS - AJOUT MODIFICATION
// ═══════════════════════════════════════════════════════════════════════
@GetMapping("/validateurs/{id}/edit")
public String editValidateurForm(@PathVariable Long id, Model model) {
    try {
        // ✅ use getById() instead of getValidateurById()
        ValidateurDTO validateur = validateurService.getById(id);

        ValidateurCreationRequest request = new ValidateurCreationRequest();
        request.setUsername(validateur.getUsername());
        request.setEmail(validateur.getEmail());
        request.setPrenom(validateur.getPrenom());
        request.setNom(validateur.getNom());
        request.setMatricule(validateur.getMatricule());     // ✅ don't forget this
        request.setTelephone(validateur.getTelephone());
        request.setType(validateur.getTypeValidateur()); // ✅ setType() not setTypeValidateur()
        request.setAgenceId(validateur.getAgenceId());

        model.addAttribute("validateur", request);
        model.addAttribute("validateurId", id);
        model.addAttribute("agences", agenceService.getAllAgences());
        model.addAttribute("types", TypeValidateur.values());

        return "admin/validateur-edit";
    } catch (Exception e) {
        model.addAttribute("error", "Validateur non trouvé: " + e.getMessage());
        return "redirect:/admin/validateurs";
    }
}

@PostMapping("/validateurs/{id}/update")
public String updateValidateur(@PathVariable Long id,
                               @ModelAttribute ValidateurCreationRequest request,
                               RedirectAttributes ra) {
    try {
        // ✅ Build a ValidateurDTO and call update() which the service already has
        ValidateurDTO dto = new ValidateurDTO();
        dto.setUsername(request.getUsername());
        dto.setEmail(request.getEmail());
        dto.setNom(request.getNom());
        dto.setPrenom(request.getPrenom());
        dto.setMatricule(request.getMatricule());
        dto.setTelephone(request.getTelephone());
        dto.setTypeValidateur(request.getType());
        dto.setAgenceId(request.getAgenceId());
        dto.setActif(true);

        validateurService.update(id, dto); // ✅ use update() instead of updateValidateur()
        ra.addFlashAttribute("success",
            "Validateur '" + request.getUsername() + "' mis à jour avec succès !");
    } catch (Exception e) {
        ra.addFlashAttribute("error", "Erreur mise à jour validateur: " + e.getMessage());
    }
    return "redirect:/admin/validateurs";
}
}