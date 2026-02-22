// Configuration Keycloak
const keycloak = new Keycloak({
    url: 'http://localhost:8080',
    realm: 'contentieux-realm',
    clientId: 'contentieux-client'
});

const API_BASE = 'http://localhost:8097';

async function init() {
    try {
        const authenticated = await keycloak.init({
            onLoad: 'login-required',
            checkLoginIframe: false
        });

        if (authenticated) {
            document.getElementById('loader').style.display = 'none';
            document.getElementById('app').style.display = 'flex';

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

function updateUI() {
    const profile = keycloak.tokenParsed;
    document.getElementById('username').innerText = profile.preferred_username || profile.name;
    document.getElementById('status-badge').innerText = 'Connecté';
    document.getElementById('status-badge').classList.add('online');

    const roles = keycloak.realmAccess ? keycloak.realmAccess.roles : [];
    document.getElementById('token-roles').innerText = roles.join(', ');
    document.getElementById('user-role').innerText = roles.includes('ADMIN') ? 'Administrateur' : 'Utilisateur';

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
            document.getElementById('api-endpoint').innerText = `GET ${endpoint}`;

            fetchFromAPI(role, endpoint);
        };
    });

    setInterval(() => {
        const timeLeft = Math.round(keycloak.tokenParsed.exp + keycloak.timeSkew - Date.now() / 1000);
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
