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

async function init() {
    // 1. Check local session
    const localToken = localStorage.getItem('local_token');
    const localProfile = JSON.parse(localStorage.getItem('local_profile'));

    if (localToken && localProfile) {
        appToken = localToken;
        appProfile = localProfile;
        await startApp();
        return;
    }

    // 2. Try Keycloak SSO
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
    if (isError) {
        console.log("Keycloak inaccessible, switching to local mode.");
    }
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

async function performLocalLogin() {
    const username = document.getElementById('login-user').value;
    const password = document.getElementById('login-pass').value;
    const errorEl = document.getElementById('login-error');

    errorEl.style.display = 'none';

    try {
        const res = await fetch(`${API_BASE}/api/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });

        if (res.ok) {
            const data = await res.json();
            appToken = data.access_token;
            appProfile = {
                preferred_username: data.username,
                roles: [data.role]
            };

            localStorage.setItem('local_token', appToken);
            localStorage.setItem('local_profile', JSON.stringify(appProfile));

            await startApp();
        } else {
            errorEl.style.display = 'block';
        }
    } catch (e) {
        errorEl.innerText = "Erreur de connexion au serveur.";
        errorEl.style.display = 'block';
    }
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
        roleName = currentAgent ? `Agent (${currentAgent.agence.nom})` : 'Agent Bancaire';
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
        default:
            container.innerHTML = `<div id="api-response" class="response-box">Sélectionnez un espace pour commencer...</div>`;
    }
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

async function renderAgentView(container) {
    container.innerHTML = `
        <div class="workflow-container">
            <div class="action-bar">
                <button class="primary-btn" onclick="toggleCreateForm()">+ Nouveau Dossier</button>
            </div>
            
            <div id="create-dossier-form" class="form-card" style="display: none; margin-bottom: 2rem;">
                <h3>Nouveau Dossier</h3>
                <div class="form-group">
                    <label>Client ID</label>
                    <input type="number" id="clientId" value="1">
                </div>
                ${!currentAgent ? `
                <div class="form-group">
                    <label>Agence ID</label>
                    <input type="number" id="agenceId" value="1">
                </div>` : `
                <div class="form-group">
                    <label>Agence (Auto)</label>
                    <div style="padding: 10px; background: rgba(255,255,255,0.05); border-radius: 8px;">
                        ${currentAgent.agence.nom}
                    </div>
                </div>
                `}
                <div class="form-actions">
                    <button class="secondary-btn" onclick="toggleCreateForm()">Annuler</button>
                    <button class="primary-btn" onclick="submitCreateDossier()">Valider</button>
                </div>
            </div>

            <div id="dossiers-list" class="list-container">Chargement des dossiers...</div>
        </div>
    `;
    loadDossiers();
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
            <div class="list-item">
                <div style="flex: 1;">
                    <strong>${d.numeroDossier}</strong> <span class="badge ${d.statut.toLowerCase()}">${d.statut}</span><br>
                    <small>Client: ${d.client.nom} - Agence: ${d.agence.nom}</small>
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

async function submitDossier(id) {
    const res = await fetch(`${API_BASE}/api/dossiers/${id}/submit`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${appToken}` }
    });
    if (res.ok) loadDossiers();
}

async function renderValidationView(container, type) {
    container.innerHTML = `<h3>Validation ${type === 'jur' ? 'Juridique' : 'Financière'}</h3>
    <div id="validation-list" class="list-container">Chargement...</div>`;

    const response = await fetch(`${API_BASE}/api/dossiers`, {
        headers: { 'Authorization': `Bearer ${appToken}` }
    });
    const dossiers = await response.json();
    const filtered = dossiers.filter(d => d.statut === 'ATTENTE_VALIDATION');

    document.getElementById('validation-list').innerHTML = filtered.map(d => `
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

init();
