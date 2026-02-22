// Configuration Keycloak
const keycloak = new Keycloak({
    url: 'http://localhost:8080',
    realm: 'contentieux-realm',
    clientId: 'contentieux-client'
});

const API_BASE = 'http://localhost:8097';
let currentAgent = null;

async function init() {
    try {
        const authenticated = await keycloak.init({
            onLoad: 'login-required',
            checkLoginIframe: false
        });

        if (authenticated) {
            document.getElementById('loader').style.display = 'none';
            document.getElementById('app').style.display = 'flex';

            await fetchAgentProfile();
            updateUI();
            setupInteractions();

            // Redirection automatique vers le premier rôle disponible
            autoRedirect();
        }
    } catch (error) {
        console.error("Erreur Keycloak:", error);
        document.getElementById('loader').style.display = 'none';
        document.getElementById('error-overlay').style.display = 'flex';
    }
}

async function fetchAgentProfile() {
    try {
        const res = await fetch(`${API_BASE}/api/admin/me`, {
            headers: { 'Authorization': `Bearer ${keycloak.token}` }
        });
        if (res.ok) {
            currentAgent = await res.json();
        }
    } catch (e) {
        console.log("Not an agent or profile not found");
    }
}

function updateUI() {
    const profile = keycloak.tokenParsed;
    document.getElementById('username').innerText = profile.preferred_username || profile.name;
    document.getElementById('status-badge').innerText = 'Connecté';
    document.getElementById('status-badge').classList.add('online');

    const roles = keycloak.realmAccess ? keycloak.realmAccess.roles : [];
    document.getElementById('token-roles').innerText = roles.join(', ');

    let roleName = 'Utilisateur';
    if (roles.includes('ADMIN')) roleName = 'Administrateur';
    else if (roles.includes('AGENT')) {
        roleName = currentAgent ? `Agent (${currentAgent.agence.nom})` : 'Agent (Non Assigné)';
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
        const res = await fetch(`${API_BASE}/api/admin/stats`, { headers: { 'Authorization': `Bearer ${keycloak.token}` } });
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
        const res = await fetch(`${API_BASE}/api/admin/agences`, { headers: { 'Authorization': `Bearer ${keycloak.token}` } });
        const agences = await res.json();
        pane.innerHTML = `
            <div class="form-card" style="margin-bottom: 24px;">
                <h4>Ajouter une Agence</h4>
                <div class="form-group"><input id="agNom" placeholder="Nom de l'agence"></div>
                <div class="form-group"><input id="agCode" placeholder="Code"></div>
                <div class="form-group"><input id="agVille" placeholder="Ville"></div>
                <button class="primary-btn" onclick="addAgence()">Ajouter</button>
            </div>
            <div class="list-container">
                ${agences.map(a => `<div class="list-item"><span>${a.nom} (${a.ville})</span> <button onclick="deleteAgence(${a.id})">Supprimer</button></div>`).join('')}
            </div>
        `;
    } else if (tab === 'agents') {
        const [agRes, agtRes] = await Promise.all([
            fetch(`${API_BASE}/api/admin/agences`, { headers: { 'Authorization': `Bearer ${keycloak.token}` } }),
            fetch(`${API_BASE}/api/admin/agents`, { headers: { 'Authorization': `Bearer ${keycloak.token}` } })
        ]);
        const agences = await agRes.json();
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
                ${agents.map(a => `<div class="list-item"><span>${a.username} (${a.email}) - ${a.agence ? a.agence.nom : 'N/A'}</span> <button onclick="deleteAgent(${a.id})">Supprimer</button></div>`).join('')}
            </div>
        `;
    }
}

// Admin Actions
async function addAgence() {
    const data = { nom: document.getElementById('agNom').value, code: document.getElementById('agCode').value, ville: document.getElementById('agVille').value };
    await fetch(`${API_BASE}/api/admin/agences`, {
        method: 'POST', headers: { 'Authorization': `Bearer ${keycloak.token}`, 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
    });
    showAdminTab('agences');
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
        method: 'POST', headers: { 'Authorization': `Bearer ${keycloak.token}`, 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
    });
    showAdminTab('agents');
}

async function deleteAgence(id) {
    if (confirm("Supprimer cette agence ?")) {
        await fetch(`${API_BASE}/api/admin/agences/${id}`, { method: 'DELETE', headers: { 'Authorization': `Bearer ${keycloak.token}` } });
        showAdminTab('agences');
    }
}

async function deleteAgent(id) {
    if (confirm("Supprimer cet agent ?")) {
        await fetch(`${API_BASE}/api/admin/agents/${id}`, { method: 'DELETE', headers: { 'Authorization': `Bearer ${keycloak.token}` } });
        showAdminTab('agents');
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
            'Authorization': `Bearer ${keycloak.token}`,
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
        headers: { 'Authorization': `Bearer ${keycloak.token}` }
    });
    if (res.ok) loadDossiers();
}

async function renderValidationView(container, type) {
    container.innerHTML = `<h3>Validation ${type === 'jur' ? 'Juridique' : 'Financière'}</h3>
    <div id="validation-list" class="list-container">Chargement...</div>`;

    const response = await fetch(`${API_BASE}/api/dossiers`, {
        headers: { 'Authorization': `Bearer ${keycloak.token}` }
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
        headers: { 'Authorization': `Bearer ${keycloak.token}` }
    });
    if (res.ok) renderRoleView(type === 'jur' ? 'juridique' : 'financier');
}

function autoRedirect() {
    const roles = keycloak.realmAccess ? keycloak.realmAccess.roles : [];
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
    document.getElementById('logout-btn').onclick = () => keycloak.logout();

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
        const profile = keycloak.tokenParsed;
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
            headers['Authorization'] = `Bearer ${keycloak.token}`;
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
