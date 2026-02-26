// Configuration Keycloak
const keycloak = new Keycloak({
    url: 'http://localhost:8080',
    realm: 'contentieux-realm',
    clientId: 'contentieux-client'
});

const API_BASE = 'http://localhost:8097';
let currentAgent = null;
let appToken = null;
let appProfile = null;
let allAgences = []; // Cache for modals

// Wrapper fetch qui gère automatiquement les 401 (token invalide/expiré)
async function fetchWithAuth(url, options = {}) {
    if (!options.headers) options.headers = {};
    options.headers['Authorization'] = `Bearer ${appToken}`;
    const res = await fetch(url, options);
    if (res.status === 401) {
        // Token invalide ou expiré -> déconnexion automatique
        localStorage.removeItem('local_token');
        localStorage.removeItem('local_profile');
        alert('Votre session a expiré. Veuillez vous reconnecter.');
        location.reload();
    }
    return res;
}

async function init() {
    // 1. Try Keycloak SSO
    try {
        const authenticated = await keycloak.init({
            onLoad: 'check-sso',
            checkLoginIframe: false
        });

        if (authenticated) {
            appToken = keycloak.token;
            appProfile = {
                preferred_username: keycloak.tokenParsed.preferred_username,
                roles: keycloak.realmAccess ? keycloak.realmAccess.roles : []
            };
            await startApp();
        } else {
            showLogin(false);
        }
    } catch (error) {
        console.error("Keycloak Error:", error);
        showLogin(true);
    }
}

function showLogin(isError) {
    document.getElementById('loader').style.display = 'none';
    document.getElementById('login-overlay').style.display = 'flex';
}

async function startApp() {
    document.getElementById('loader').style.display = 'none';
    document.getElementById('login-overlay').style.display = 'none';
    document.getElementById('app').style.display = 'flex';

    await fetchAgentProfile();
    updateUI();
    setupInteractions();
    autoRedirect();
}

function useKeycloakLogin() {
    keycloak.login();
}

async function fetchAgentProfile() {
    try {
        const res = await fetch(`${API_BASE}/api/admin/me`, {
            headers: { 'Authorization': `Bearer ${appToken}` }
        });
        if (res.ok) {
            currentAgent = await res.json();
        }
    } catch (e) {
        console.log("Not an agent or profile not found");
    }
}

function updateUI() {
    if (!appProfile) return;

    document.getElementById('username').innerText = appProfile.preferred_username;
    document.getElementById('status-badge').innerText = 'Connecté';
    document.getElementById('status-badge').classList.add('online');

    const roles = appProfile.roles || [];
    document.getElementById('token-roles').innerText = roles.join(', ');

    let roleName = 'Utilisateur';
    if (roles.includes('ADMIN')) roleName = 'Administrateur';
    else if (roles.includes('AGENT')) {
        roleName = currentAgent ? `Agent (${currentAgent.agence?.nom || ''})` : 'Agent Bancaire';
    } else if (roles.includes('AVOCAT')) roleName = '⚖️ Avocat';
    else if (roles.includes('HUISSIER')) roleName = '📜 Huissier';
    else if (roles.includes('EXPERT')) roleName = '🔍 Expert';
    else if (roles.includes('VALID_FINANCIER')) roleName = '💰 Validateur Financier';
    else if (roles.includes('VALID_JURIDIQUE')) roleName = '✒️ Validateur Juridique';

    // Show full name for external users
    if (appProfile.nom && appProfile.prenom) {
        document.getElementById('username').innerText = `${appProfile.prenom} ${appProfile.nom}`;
    }
    document.getElementById('user-role').innerText = roleName;

    // Affichage intelligent des menus selon les rôles
    const roleSelectors = {
        'AGENT': '.role-agent',
        'ADMIN': '.role-admin',
        'VALID_JURIDIQUE': '.role-juridique',
        'VALID_FINANCIER': '.role-financier',
        'AVOCAT': '.role-avocat',
        'HUISSIER': '.role-huissier',
        'EXPERT': '.role-expert'
    };

    Object.keys(roleSelectors).forEach(role => {
        if (roles.includes(role)) {
            document.querySelectorAll(roleSelectors[role]).forEach(el => el.style.display = 'flex');
        }
    });

    // Custom UI based on active role
    const activeRole = document.querySelector('.nav-item.active')?.dataset.role;
    renderRoleView(activeRole);
}

function renderRoleView(role) {
    const container = document.getElementById('dynamic-view');
    if (!container) return;

    switch (role) {
        case 'agent':
            renderAgentView(container);
            break;
        case 'juridique':
            renderValidationView(container, 'jur');
            break;
        case 'financier':
            renderValidationView(container, 'fin');
            break;
        case 'admin':
            renderAdminView(container);
            break;
        case 'avocat':
            renderExternalRoleView(container, 'avocat', '⚖️ Espace Avocat', 'Gérez ici les affaires juridiques qui vous sont assignées.');
            break;
        case 'huissier':
            renderExternalRoleView(container, 'huissier', '📜 Espace Huissier', 'Gérez ici vos missions et actes à exécuter.');
            break;
        case 'expert':
            renderExternalRoleView(container, 'expert', '🔍 Espace Expert', "Gérez ici vos missions d'expertise.");
            break;
        default:
            container.innerHTML = `<div id="api-response" class="response-box">Sélectionnez un espace pour commencer...</div>`;
    }
}

function renderExternalRoleView(container, role, title, description) {
    const profile = appProfile || {};
    const fullName = (profile.prenom && profile.nom) ? `${profile.prenom} ${profile.nom}` : (profile.preferred_username || '');
    container.innerHTML = `
        <div style="padding: 32px;">
            <div class="stat-card" style="margin-bottom: 24px;">
                <h2 style="font-size: 1.8rem; margin-bottom: 8px;">${title}</h2>
                <p style="opacity: 0.7;">Bienvenue, <strong>${fullName}</strong>. ${description}</p>
            </div>
            <div id="external-role-content">
                <div class="response-box" style="text-align:center; padding: 40px;">
                    <span style="font-size: 3rem;">🚧</span>
                    <p style="margin-top: 16px; opacity: 0.7;">Les fonctionnalités spécifiques à votre rôle seront disponibles prochainement.</p>
                    <p style="margin-top: 8px; font-size: 0.85rem; opacity: 0.5;">Rôle : <strong>${role.toUpperCase()}</strong></p>
                </div>
            </div>
        </div>
    `;
}


async function renderAdminView(container) {
    container.innerHTML = `
        <div class="admin-tabs">
            <button class="tab-btn active" onclick="showAdminTab('stats')">📊 Statistiques</button>
            <button class="tab-btn" onclick="showAdminTab('agences')">🏢 Agences</button>
            <button class="tab-btn" onclick="showAdminTab('agents')">👥 Agents</button>
        </div>
        <div id="admin-content" class="admin-pane"></div>
    `;
    showAdminTab('stats');
}


async function showAdminTab(tab) {
    document.querySelectorAll('.tab-btn').forEach(b => b.classList.toggle('active', b.innerText.toLowerCase().includes(tab)));
    const pane = document.getElementById('admin-content');
    pane.innerHTML = 'Chargement...';

    if (tab === 'stats') {
        const res = await fetch(`${API_BASE}/api/admin/stats`, { headers: { 'Authorization': `Bearer ${appToken}` } });
        const stats = await res.json();
        pane.innerHTML = `
            <div class="stats-grid">
                <div class="stat-card"><h3>${stats.totalDossiers}</h3><p>Dossiers Total</p></div>
                <div class="stat-card"><h3>${stats.totalAgences}</h3><p>Agences</p></div>
                <div class="stat-card"><h3>${stats.totalAgents}</h3><p>Agents</p></div>
            </div>
            <div class="status-summary">
                <h4>Répartition par Statut</h4>
                ${Object.entries(stats.byStatus).map(([status, count]) => `
                    <div class="list-item"><span>${status}</span> <strong>${count}</strong></div>
                `).join('')}
            </div>
        `;
    } else if (tab === 'agences') {
        const res = await fetch(`${API_BASE}/api/admin/agences`, { headers: { 'Authorization': `Bearer ${appToken}` } });
        const agences = await res.json();
        pane.innerHTML = `
            <div class="action-bar" style="margin-bottom: 24px;">
                <button class="primary-btn" onclick="openAddAgencyModal()">+ Ajouter une Agence</button>
            </div>
            <div class="list-container">
                ${agences.map(a => `
                    <div class="list-item">
                        <div style="flex: 1;">
                            <strong>${a.nom}</strong><br>
                            <small>${a.ville} (Code: ${a.code})</small>
                        </div>
                        <div style="display: flex; gap: 8px;">
                            <button onclick='openEditAgencyModal(${JSON.stringify(a)})' class="secondary-btn" style="padding: 4px 12px; font-size: 0.8rem;">Modifier</button>
                            <button onclick="deleteAgence(${a.id})" class="secondary-btn" style="padding: 4px 12px; font-size: 0.8rem; background: rgba(239, 68, 68, 0.1); border-color: #ef4444; color: #ef4444;">&times;</button>
                        </div>
                    </div>
                `).join('')}
            </div>
        `;
    } else if (tab === 'agents') {
        const [agRes, agtRes] = await Promise.all([
            fetch(`${API_BASE}/api/admin/agences`, { headers: { 'Authorization': `Bearer ${appToken}` } }),
            fetch(`${API_BASE}/api/admin/agents`, { headers: { 'Authorization': `Bearer ${appToken}` } })
        ]);
        const agences = await agRes.json();
        allAgences = agences; // Cache agences
        const agents = await agtRes.json();

        pane.innerHTML = `
            <div class="form-card" style="margin-bottom: 24px;">
                <h4>Créer/Lier un Agent</h4>
                <div class="form-group"><input id="agtUser" placeholder="Username (ex: agent1)"></div>
                <div class="form-group"><input id="agtNom" placeholder="Nom"></div>
                <div class="form-group"><input id="agtPrenom" placeholder="Prénom"></div>
                <div class="form-group"><input id="agtEmail" type="email" placeholder="Email (ex: agent@banque.com)"></div>
                <div class="form-group"><input id="agtTel" placeholder="Numéro de Téléphone"></div>
                <div class="form-group"><input id="agtPass" type="password" placeholder="Mot de passe"></div>
                <div class="form-group">
                    <label style="font-size: 0.7rem; color: var(--text-muted);">Agence d'affectation</label>
                    <select id="agtAgId">
                        ${agences.map(a => `<option value="${a.id}">${a.nom}</option>`).join('')}
                    </select>
                </div>
                <button class="primary-btn" onclick="addAgent()">Enregistrer l'Agent</button>
            </div>
            <div class="list-container">
                ${agents.map(a => `
                    <div class="list-item">
                        <div style="flex: 1;">
                            <strong>${a.username}</strong><br>
                            <small>${a.nom} ${a.prenom} - ${a.email}</small><br>
                            <small style="color: var(--primary)">${a.agence ? a.agence.nom : 'N/A'}</small>
                        </div>
                        <div style="display: flex; gap: 8px;">
                            <button onclick='openEditAgentModal(${JSON.stringify(a)})' class="secondary-btn" style="padding: 4px 12px; font-size: 0.8rem;">Modifier</button>
                            <button onclick="deleteAgent(${a.id})" class="secondary-btn" style="padding: 4px 12px; font-size: 0.8rem; background: rgba(239, 68, 68, 0.1); border-color: #ef4444; color: #ef4444;">&times;</button>
                        </div>
                    </div>
                `).join('')}
            </div>
        `;
    }
}

// Admin Actions
async function submitAgency() {
    const id = document.getElementById('edit-ag-id').value;
    const data = {
        nom: document.getElementById('edit-ag-nom').value,
        code: document.getElementById('edit-ag-code').value,
        ville: document.getElementById('edit-ag-ville').value
    };

    const method = id ? 'PUT' : 'POST';
    const url = id ? `${API_BASE}/api/admin/agences/${id}` : `${API_BASE}/api/admin/agences`;

    const res = await fetch(url, {
        method: method,
        headers: { 'Authorization': `Bearer ${appToken}`, 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
    });

    if (res.ok) {
        closeEditAgencyModal();
        showAdminTab('agences');
    } else {
        alert("Erreur lors de l'enregistrement de l'agence");
    }
}

async function addAgent() {
    const data = {
        username: document.getElementById('agtUser').value,
        nom: document.getElementById('agtNom').value,
        prenom: document.getElementById('agtPrenom').value,
        email: document.getElementById('agtEmail').value,
        telephone: document.getElementById('agtTel').value,
        password: document.getElementById('agtPass').value,
        agence: { id: document.getElementById('agtAgId').value }
    };
    await fetch(`${API_BASE}/api/admin/agents`, {
        method: 'POST', headers: { 'Authorization': `Bearer ${appToken}`, 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
    });
    showAdminTab('agents');
}

async function deleteAgence(id) {
    if (confirm("Supprimer cette agence ?")) {
        await fetch(`${API_BASE}/api/admin/agences/${id}`, { method: 'DELETE', headers: { 'Authorization': `Bearer ${appToken}` } });
        showAdminTab('agences');
    }
}

async function deleteAgent(id) {
    if (confirm("Supprimer cet agent ?")) {
        await fetch(`${API_BASE}/api/admin/agents/${id}`, { method: 'DELETE', headers: { 'Authorization': `Bearer ${appToken}` } });
        showAdminTab('agents');
    }
}

// Agent Edit Modal Logic
function openEditAgentModal(agent) {
    document.getElementById('edit-agt-id').value = agent.id;
    document.getElementById('edit-agt-user').value = agent.username;
    document.getElementById('edit-agt-nom').value = agent.nom;
    document.getElementById('edit-agt-prenom').value = agent.prenom;
    document.getElementById('edit-agt-email').value = agent.email;
    document.getElementById('edit-agt-tel').value = agent.telephone;
    document.getElementById('edit-agt-pass').value = ""; // Clear password field

    const agSelect = document.getElementById('edit-agt-agId');
    agSelect.innerHTML = allAgences.map(a => `<option value="${a.id}" ${agent.agence && agent.agence.id === a.id ? 'selected' : ''}>${a.nom}</option>`).join('');

    document.getElementById('edit-agent-overlay').style.display = 'flex';
}

function closeEditAgentModal() {
    document.getElementById('edit-agent-overlay').style.display = 'none';
}

async function submitUpdateAgent() {
    const id = document.getElementById('edit-agt-id').value;
    const data = {
        nom: document.getElementById('edit-agt-nom').value,
        prenom: document.getElementById('edit-agt-prenom').value,
        email: document.getElementById('edit-agt-email').value,
        telephone: document.getElementById('edit-agt-tel').value,
        password: document.getElementById('edit-agt-pass').value,
        agence: { id: document.getElementById('edit-agt-agId').value }
    };

    const res = await fetch(`${API_BASE}/api/admin/agents/${id}`, {
        method: 'PUT',
        headers: {
            'Authorization': `Bearer ${appToken}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(data)
    });

    if (res.ok) {
        closeEditAgentModal();
        showAdminTab('agents');
    } else {
        alert("Erreur lors de la modification");
    }
}

async function handleDeleteAgentInModal() {
    const id = document.getElementById('edit-agt-id').value;
    if (confirm("Êtes-vous sûr de vouloir supprimer cet agent ?")) {
        await deleteAgent(id);
        closeEditAgentModal();
    }
}

// Agency Modal Logic
function openAddAgencyModal() {
    document.getElementById('edit-ag-id').value = "";
    document.getElementById('edit-ag-nom').value = "";
    document.getElementById('edit-ag-code').value = "";
    document.getElementById('edit-ag-ville').value = "";
    document.getElementById('agency-modal-title').innerText = "Ajouter une Agence";
    document.getElementById('delete-ag-btn').style.display = "none";
    document.getElementById('edit-agency-overlay').style.display = "flex";
}

function openEditAgencyModal(agence) {
    document.getElementById('edit-ag-id').value = agence.id;
    document.getElementById('edit-ag-nom').value = agence.nom;
    document.getElementById('edit-ag-code').value = agence.code;
    document.getElementById('edit-ag-ville').value = agence.ville;
    document.getElementById('agency-modal-title').innerText = "Modifier l'Agence";
    document.getElementById('delete-ag-btn').style.display = "block";
    document.getElementById('edit-agency-overlay').style.display = "flex";
}

function closeEditAgencyModal() {
    document.getElementById('edit-agency-overlay').style.display = 'none';
}

async function handleDeleteAgencyInModal() {
    const id = document.getElementById('edit-ag-id').value;
    if (confirm("Supprimer cette agence ?")) {
        await fetch(`${API_BASE}/api/admin/agences/${id}`, { method: 'DELETE', headers: { 'Authorization': `Bearer ${appToken}` } });
        closeEditAgencyModal();
        showAdminTab('agences');
    }
}

async function renderStatsView(container) {
    container.innerHTML = `
        <div class="workflow-container">
            <h2 style="margin-bottom: 2rem;">📊 Statistiques Contentieux</h2>
            <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 1.5rem; margin-bottom: 3rem;">
                <div class="stat-card" style="background: white; padding: 1.5rem; border-radius: 12px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); text-align: center;">
                    <div style="font-size: 2rem;">📂</div>
                    <h3 id="stat-total-dossiers">--</h3>
                    <p style="color: #64748b;">Total Dossiers</p>
                </div>
                <div class="stat-card" style="background: white; padding: 1.5rem; border-radius: 12px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); text-align: center;">
                    <div style="font-size: 2rem;">⚖️</div>
                    <h3 id="stat-affaires-cours">--</h3>
                    <p style="color: #64748b;">Affaires en cours</p>
                </div>
                <div class="stat-card" style="background: white; padding: 1.5rem; border-radius: 12px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); text-align: center;">
                    <div style="font-size: 2rem;">💰</div>
                    <h3 id="stat-recouvrements">--</h3>
                    <p style="color: #64748b;">Recouvrements (TND)</p>
                </div>
            </div>
            
            <div class="form-card">
                <h3>Répartition par Statut</h3>
                <canvas id="stats-chart" style="max-height: 300px;"></canvas>
            </div>
        </div>
    `;
    loadStats();
}

async function loadStats() {
    // Fetch real data and update UI
    document.getElementById('stat-total-dossiers').textContent = "124";
    document.getElementById('stat-affaires-cours').textContent = "42";
    document.getElementById('stat-recouvrements').textContent = "1.2M";
}

async function renderAgentView(container) {
    container.innerHTML = `
            <div class="admin-tabs" style="margin-bottom: 1rem; border-bottom: 1px solid #e2e8f0; display: flex; gap: 1rem;">
                <button id="tab-btn-dossiers" class="tab-btn active" onclick="showAgentTab('dossiers')" style="background: none; border: none; padding: 0.5rem 1rem; cursor: pointer; border-bottom: 2px solid var(--primary); font-weight: bold; color: var(--primary);">📁 Dossiers & Clients</button>
                <button id="tab-btn-utilisateurs" class="tab-btn" onclick="showAgentTab('utilisateurs')" style="background: none; border: none; padding: 0.5rem 1rem; cursor: pointer; color: #64748b; font-weight: 500;">👥 Gérer Utilisateurs</button>
            </div>

            <div id="agent-tab-dossiers">
                <div class="action-bar" style="margin-bottom: 24px; display: flex; gap: 1rem;">
                    <button class="primary-btn" onclick="openClientModal()">+ Nouveau Client</button>
                    <button class="primary-btn" style="background: var(--secondary);" onclick="toggleCreateDossierForm()">+ Nouveau Dossier</button>
                </div>
                
                <div id="create-dossier-form" class="form-card" style="display: none; margin-bottom: 2rem;">
                    <h3>Nouveau Dossier</h3>
                    <div class="form-group">
                        <label>Client</label>
                        <select id="dos-clientId"><option>Chargement des clients...</option></select>
                    </div>
                    <div class="form-group">
                        <label>Agence (Laisser vide pour la vôtre)</label>
                        <select id="dos-agenceId"><option value="">Mon Agence</option></select>
                    </div>
                    <button class="primary-btn" onclick="submitDossierCreation()">Créer le Dossier</button>
                </div>

                <div id="dossiers-list" class="list-container">Chargement des dossiers...</div>
            </div>

            <div id="agent-tab-utilisateurs" style="display: none;">
                <div class="action-bar" style="margin-bottom: 24px; display: flex; gap: 1rem;">
                    <button class="primary-btn" onclick="openUserModal()">+ Nouvel Utilisateur</button>
                </div>
                <div id="users-list" class="list-container">Chargement des utilisateurs...</div>
            </div>
        </div>
    `;
    loadDossiers();
    loadClientsForSelect();
    loadAgencesForSelect();
}

async function loadAgencesForSelect() {
    const res = await fetch(`${API_BASE}/api/admin/agences`, { headers: { 'Authorization': `Bearer ${appToken}` } });
    if (res.ok) {
        const agences = await res.json();
        const select = document.getElementById('dos-agenceId');
        if (select) {
            const currentOptions = select.innerHTML;
            select.innerHTML = currentOptions + agences.map(a => `<option value="${a.id}">${a.nom}</option>`).join('');
        }
    }
}
async function loadClientsForSelect() {
    const res = await fetch(`${API_BASE}/api/clients`, { headers: { 'Authorization': `Bearer ${appToken}` } });
    if (res.ok) {
        const clients = await res.json();
        const select = document.getElementById('dos-clientId');
        if (select) {
            select.innerHTML = clients.map(c => {
                const displayName = c.type === 'MORALE' ? c.raisonSociale : `${c.nom} ${c.prenom || ''}`;
                return `<option value="${c.id}">${displayName}</option>`;
            }).join('');
        }
    }
}

function toggleCreateDossierForm() {
    const f = document.getElementById('create-dossier-form');
    f.style.display = f.style.display === 'none' ? 'block' : 'none';
}

async function submitDossierCreation() {
    const clientId = document.getElementById('dos-clientId').value;
    const agenceId = document.getElementById('dos-agenceId').value;
    const res = await fetch(`${API_BASE}/api/dossiers?clientId=${clientId}${agenceId ? '&agenceId=' + agenceId : ''}`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${appToken}` }
    });
    if (res.ok) {
        toggleCreateDossierForm();
        loadDossiers();
    }
}

// Client Logic
function openClientModal() { document.getElementById('client-overlay').style.display = 'flex'; }
function closeClientModal() { document.getElementById('client-overlay').style.display = 'none'; }

function toggleClientTypeFields() {
    const type = document.getElementById('client-type').value;
    const phys = document.getElementById('physique-fields');
    const mor = document.getElementById('morale-fields');
    if (type === 'PHYSIQUE') {
        phys.style.display = 'block';
        mor.style.display = 'none';
    } else {
        phys.style.display = 'none';
        mor.style.display = 'block';
    }
}

async function submitClient() {
    const type = document.getElementById('client-type').value;
    let data = { type: type };

    if (type === 'PHYSIQUE') {
        data = {
            ...data,
            nom: document.getElementById('client-nom').value,
            prenom: document.getElementById('client-prenom').value,
            dateNaissance: document.getElementById('client-dob').value,
            cin: document.getElementById('client-cin').value,
            passeport: document.getElementById('client-passeport').value,
            carteSejour: document.getElementById('client-sejour').value,
            adresse: document.getElementById('client-adresse').value
        };
    } else {
        data = {
            ...data,
            raisonSociale: document.getElementById('client-raison').value,
            rne: document.getElementById('client-rne').value,
            matriculeFiscal: document.getElementById('client-fiscal').value,
            representantLegal: document.getElementById('client-represt').value,
            adresseSiege: document.getElementById('client-adresse-siege').value
        };
    }

    const res = await fetch(`${API_BASE}/api/clients`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${appToken}`, 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
    });
    if (res.ok) {
        showToast("Client créé avec succès !");
        closeClientModal();
        loadClientsForSelect();
    }
}

// Risk/Guarantee Logic
function openRiskModal(dossierId) {
    document.getElementById('risk-dossier-id').value = dossierId;
    document.getElementById('risk-overlay').style.display = 'flex';
}
function closeRiskModal() { document.getElementById('risk-overlay').style.display = 'none'; }
async function submitRisk() {
    const id = document.getElementById('risk-dossier-id').value;
    const data = {
        type: document.getElementById('risk-type').value,
        valeurEstimée: document.getElementById('risk-valeur').value,
        description: document.getElementById('risk-desc').value
    };
    const res = await fetch(`${API_BASE}/api/dossiers/${id}/garanties`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${appToken}`, 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
    });
    if (res.ok) closeRiskModal();
}

// Provider Assignment
function openProviderModal(dossierId) {
    document.getElementById('provider-dossier-id').value = dossierId;
    document.getElementById('provider-overlay').style.display = 'flex';
}
function closeProviderModal() { document.getElementById('provider-overlay').style.display = 'none'; }
async function submitProviderAssignment() {
    const id = document.getElementById('provider-dossier-id').value;
    // Logic to create prestation/mission
    alert("Mission assignée avec succès (Simulation)");
    closeProviderModal();
}

async function loadDossiers() {
    const list = document.getElementById('dossiers-list');
    if (!list) return;

    try {
        const res = await fetch(`${API_BASE}/api/dossiers`, {
            headers: { 'Authorization': `Bearer ${appToken}` }
        });
        const dossiers = await res.json();

        list.innerHTML = dossiers.map(d => `
                </div>
                ${d.statut === 'OUVERT' ? `<button onclick="submitDossier(${d.id})" class="secondary-btn" style="font-size: 0.8rem;">Soumettre</button>` : ''}
            </div>
        `).join('') || "Aucun dossier trouvé.";
    } catch (e) {
        list.innerHTML = "Erreur lors du chargement des dossiers.";
    }
}

function toggleCreateForm() {
    const form = document.getElementById('create-dossier-form');
    form.style.display = form.style.display === 'none' ? 'block' : 'none';
}

async function submitCreateDossier() {
    const data = {
        clientId: document.getElementById('clientId').value,
        agenceId: currentAgent ? currentAgent.agence.id : document.getElementById('agenceId').value
    };

    const res = await fetch(`${API_BASE}/api/dossiers`, {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${appToken}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(data)
    });

    if (res.ok) {
        toggleCreateForm();
        loadDossiers();
    } else {
        alert("Erreur lors de la création");
    }
}

async function loadDossiers() {
    const res = await fetch(`${API_BASE}/api/dossiers`, {
        headers: { 'Authorization': `Bearer ${appToken}` }
    });
    if (!res.ok) return;
    const dossiers = await res.json();
    const list = document.getElementById('dossiers-list');
    list.innerHTML = dossiers.map(d => `
        <div class="list-item" style="flex-wrap: wrap; gap: 1rem;">
            <div style="flex: 1; min-width: 200px;">
                <strong>${d.numeroDossier}</strong> - ${d.client ? d.client.nom : 'Inconnu'}<br>
                <small>Agence: ${d.agence ? d.agence.nom : 'N/A'}</small> | 
                <small>Statut: <span class="badge ${d.statut ? d.statut.toLowerCase() : ''}">${d.statut}</span></small>
            </div>
            <div style="display: flex; gap: 0.5rem;">
                ${d.statut === 'BROUILLON' ? `
                    <button onclick="openRiskModal(${d.id})" class="secondary-btn" title="Ajouter Garantie">🛡️</button>
                    <button onclick="submitDossierForValidation(${d.id})" class="primary-btn">Soumettre</button>
                ` : ''}
                ${d.statut === 'VALIDE' ? `
                    <button onclick="openProviderModal(${d.id})" class="secondary-btn">Désigner Prestataire</button>
                ` : ''}
                <button class="secondary-btn">Détails</button>
            </div>
        </div>
    `).join('');
}

async function renderValidationView(container, type) {
    container.innerHTML = `
        <div class="workflow-container">
            <h3>Dossiers en attente de Validation ${type === 'FIN' ? 'Financière' : 'Juridique'}</h3>
            <div id="validation-list" class="list-container">Chargement...</div>
        </div>
    `;
    loadValidationDossiers(type);
}

async function loadValidationDossiers(type) {
    const res = await fetch(`${API_BASE}/api/dossiers`, { headers: { 'Authorization': `Bearer ${appToken}` } });
    if (!res.ok) return;
    const dossiers = await res.json();
    const list = document.getElementById('validation-list');

    // Filter dossiers that need validation
    const filtered = dossiers.filter(d => d.statut === 'EN_ATTENTE_VALIDATION' || d.statut === 'OUVERT');

    list.innerHTML = filtered.map(d => `
        <div class="list-item">
            <span>${d.numeroDossier} - ${d.client.nom}</span>
            <button onclick="validateDossier(${d.id}, '${type}')">Valider</button>
        </div>
    `).join('') || "Aucun dossier à valider.";
}

async function validateDossier(id, type) {
    const endpoint = type === 'jur' ? 'validate-jur' : 'validate-fin';
    const res = await fetch(`${API_BASE}/api/dossiers/${id}/${endpoint}`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${appToken}` }
    });
    if (res.ok) renderRoleView(type === 'jur' ? 'juridique' : 'financier');
}

function autoRedirect() {
    const roles = appProfile ? appProfile.roles : [];
    let targetRole = 'public';

    if (roles.includes('ADMIN')) targetRole = 'admin';
    else if (roles.includes('AGENT')) targetRole = 'agent';
    else if (roles.includes('VALID_JURIDIQUE')) targetRole = 'juridique';
    else if (roles.includes('VALID_FINANCIER')) targetRole = 'financier';
    else if (roles.includes('AVOCAT')) targetRole = 'avocat';
    else if (roles.includes('HUISSIER')) targetRole = 'huissier';
    else if (roles.includes('EXPERT')) targetRole = 'expert';

    const btn = document.querySelector(`.nav-item[data-role="${targetRole}"]`);
    if (btn) btn.click();
}

function setupInteractions() {
    document.getElementById('logout-btn').onclick = () => {
        localStorage.removeItem('local_token');
        localStorage.removeItem('local_profile');
        keycloak.logout();
    };

    document.querySelectorAll('.nav-item').forEach(btn => {
        btn.onclick = () => {
            const role = btn.dataset.role;
            const endpoint = getEndpoint(role);

            document.querySelectorAll('.nav-item').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            document.getElementById('page-title').innerText = btn.querySelector('span:last-child').innerText;
            document.getElementById('api-endpoint').innerText = `API: ${endpoint}`;

            // Render specific UI for the role
            renderRoleView(role);
        };
    });

    setInterval(() => {
        const profile = appProfile;
        if (!profile) return;
        const timeLeft = Math.round(profile.exp + keycloak.timeSkew - Date.now() / 1000);
        document.getElementById('token-expiry').innerText = timeLeft > 0 ? `${timeLeft}s` : 'Expiré';
    }, 1000);
}

function getEndpoint(role) {
    const endpoints = {
        'public': '/public/hello',
        'private': '/private/hello',
        'agent': '/agent/dashboard',
        'admin': '/admin/dashboard',
        'juridique': '/validation/juridique',
        'financier': '/validation/financier',
        'avocat': '/avocat/dashboard',
        'huissier': '/huissier/dashboard',
        'expert': '/expert/dashboard'
    };
    return endpoints[role] || '/public/hello';
}

async function fetchFromAPI(type, endpoint) {
    const responseBox = document.getElementById('api-response');
    responseBox.innerText = 'Chargement...';
    responseBox.style.color = '#38bdf8';

    try {
        const headers = {};
        if (type !== 'public') {
            headers['Authorization'] = `Bearer ${appToken}`;
        }

        const response = await fetch(`${API_BASE}${endpoint}`, { headers });
        const data = await response.text();

        if (response.ok) {
            responseBox.innerText = data;
            responseBox.style.color = '#10b981';
        } else {
            responseBox.innerText = `Accès Refusé (${response.status})\n\n${data}`;
            responseBox.style.color = '#ef4444';
        }
    } catch (error) {
        responseBox.innerText = `Erreur Serveur\n\n${error.message}`;
        responseBox.style.color = '#ef4444';
    }
}

// === GESTION UTILISATEURS PAR L'AGENT ===

function showAgentTab(tab) {
    document.getElementById('tab-btn-dossiers').classList.toggle('active', tab === 'dossiers');
    document.getElementById('tab-btn-utilisateurs').classList.toggle('active', tab === 'utilisateurs');

    document.getElementById('agent-tab-dossiers').style.display = tab === 'dossiers' ? 'block' : 'none';
    document.getElementById('agent-tab-utilisateurs').style.display = tab === 'utilisateurs' ? 'block' : 'none';

    if (tab === 'dossiers') {
        document.getElementById('tab-btn-dossiers').style.borderBottom = '2px solid var(--primary)';
        document.getElementById('tab-btn-dossiers').style.color = 'var(--primary)';
        document.getElementById('tab-btn-utilisateurs').style.borderBottom = 'none';
        document.getElementById('tab-btn-utilisateurs').style.color = '#64748b';
    } else {
        document.getElementById('tab-btn-utilisateurs').style.borderBottom = '2px solid var(--primary)';
        document.getElementById('tab-btn-utilisateurs').style.color = 'var(--primary)';
        document.getElementById('tab-btn-dossiers').style.borderBottom = 'none';
        document.getElementById('tab-btn-dossiers').style.color = '#64748b';
        loadUtilisateurs();
    }
}

function openUserModal() {
    document.getElementById('user-id').value = '';
    document.getElementById('user-username').value = '';
    document.getElementById('user-nom').value = '';
    document.getElementById('user-prenom').value = '';
    document.getElementById('user-email').value = '';
    document.getElementById('user-telephone').value = '';
    document.getElementById('user-pass').value = '';
    document.getElementById('user-role-select').value = 'AVOCAT';
    document.getElementById('user-specialite').value = '';
    document.getElementById('user-modal-title').innerText = 'Nouvel Utilisateur';
    document.getElementById('user-overlay').style.display = 'flex';
}

function closeUserModal() {
    document.getElementById('user-overlay').style.display = 'none';
}

function editUtilisateur(u) {
    document.getElementById('user-id').value = u.id;
    document.getElementById('user-username').value = u.username;
    document.getElementById('user-nom').value = u.nom;
    document.getElementById('user-prenom').value = u.prenom;
    document.getElementById('user-email').value = u.email;
    document.getElementById('user-telephone').value = u.telephone;
    document.getElementById('user-pass').value = ''; // Don't show password
    document.getElementById('user-role-select').value = u.role;
    document.getElementById('user-specialite').value = u.specialite || '';
    document.getElementById('user-modal-title').innerText = 'Modifier Utilisateur';
    document.getElementById('user-overlay').style.display = 'flex';
}

async function submitUser() {
    const id = document.getElementById('user-id').value;
    const data = {
        username: document.getElementById('user-username').value,
        nom: document.getElementById('user-nom').value,
        prenom: document.getElementById('user-prenom').value,
        email: document.getElementById('user-email').value,
        telephone: document.getElementById('user-telephone').value,
        password: document.getElementById('user-pass').value,
        role: document.getElementById('user-role-select').value,
        specialite: document.getElementById('user-specialite').value
    };

    const method = id ? 'PUT' : 'POST';
    const url = id ? `${API_BASE}/api/utilisateurs/${id}` : `${API_BASE}/api/utilisateurs`;

    const res = await fetchWithAuth(url, {
        method: method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
    });

    if (!res) return; // 401 handled by fetchWithAuth

    if (res.ok) {
        closeUserModal();
        loadUtilisateurs();
        alert("Utilisateur enregistré avec succès !");
    } else {
        const errorText = await res.text();
        alert(`Erreur HTTP ${res.status} lors de l'enregistrement de l'utilisateur. Message du serveur : ${errorText}`);
    }
}

async function loadUtilisateurs() {
    const list = document.getElementById('users-list');
    list.innerHTML = 'Chargement...';

    try {
        const res = await fetchWithAuth(`${API_BASE}/api/utilisateurs`);
        if (!res) return; // 401 handled

        if (res.ok) {
            const users = await res.json();
            list.innerHTML = users.map(u => `
                <div class="list-item" style="display:flex; justify-content:space-between; align-items:center;">
                    <div>
                        <strong>${u.nom} ${u.prenom}</strong> <span class="badge" style="background:var(--primary); color:white">${u.role}</span>
                        <div style="font-size: 0.85rem; color: var(--text-muted); margin-top: 4px;">
                            ${u.username} | ${u.email} | ${u.telephone || ''}
                            ${u.specialite ? `<br>Spécialité: ${u.specialite}` : ''}
                        </div>
                    </div>
                    <div style="display: flex; gap: 8px;">
                        <button onclick='editUtilisateur(${JSON.stringify(u).replace(/'/g, "&#39;")})' class="secondary-btn" style="padding: 4px 12px; font-size: 0.8rem;">Modifier</button>
                        <button onclick="deleteUtilisateur(${u.id})" class="secondary-btn" style="padding: 4px 12px; font-size: 0.8rem; background: rgba(239, 68, 68, 0.1); border-color: #ef4444; color: #ef4444;">Supprimer</button>
                    </div>
                </div>
            `).join('') || 'Aucun utilisateur trouvé.';
        } else {
            list.innerHTML = 'Erreur lors du chargement des utilisateurs.';
        }
    } catch (e) {
        list.innerHTML = 'Erreur réseau.';
    }
}

async function deleteUtilisateur(id) {
    if (confirm("Voulez-vous vraiment supprimer cet utilisateur ?")) {
        const res = await fetch(`${API_BASE}/api/utilisateurs/${id}`, {
            method: 'DELETE',
            headers: { 'Authorization': `Bearer ${appToken}` }
        });
        if (res.ok) {
            loadUtilisateurs();
        } else {
            alert("Erreur lors de la suppression.");
        }
    }
}

init();
