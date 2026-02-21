// Configuration Keycloak
const keycloak = new Keycloak({
    url: 'http://localhost:8080',
    realm: 'contentieux-realm',
    clientId: 'contentieux-client' // Assurez-vous que ce client est AU MOINS de type "Public" ou "OIDC"
});

const API_BASE = 'http://localhost:8097';

async function init() {
    try {
        const authenticated = await keycloak.init({
            onLoad: 'login-required',
            checkLoginIframe: false
        });

        if (authenticated) {
            console.log("Connecté !");
            document.getElementById('loader').style.display = 'none';
            document.getElementById('app').style.display = 'flex';
            
            updateUI();
            setupInteractions();
            // Test initial avec l'admin ou public
            fetchFromAPI('public', '/public/hello');
        }
    } catch (error) {
        console.error("Erreur d'initialisation Keycloak:", error);
        document.getElementById('loader').style.display = 'none';
        document.getElementById('error-overlay').style.display = 'flex';
        document.getElementById('error-message').innerText = 
            "Impossible de contacter Keycloak. Vérifiez que Keycloak est lancé sur le port 8080 et que le realm 'contentieux-realm' existe.";
    }
}

function updateUI() {
    const profile = keycloak.tokenParsed;
    document.getElementById('username').innerText = profile.preferred_username || profile.name;
    document.getElementById('status-badge').innerText = 'Online';
    document.getElementById('status-badge').classList.add('online');

    const roles = keycloak.realmAccess ? keycloak.realmAccess.roles : [];
    document.getElementById('user-role').innerText = roles.includes('ADMIN') ? 'Administrateur' : 'Utilisateur';
    document.getElementById('token-roles').innerText = roles.join(', ');

    // Afficher les menus selon les rôles
    if (roles.includes('AGENT')) document.querySelectorAll('.role-agent').forEach(el => el.style.display = 'flex');
    if (roles.includes('ADMIN')) document.querySelectorAll('.role-admin').forEach(el => el.style.display = 'flex');
    if (roles.includes('VALID_JURIDIQUE')) document.querySelectorAll('.role-juridique').forEach(el => el.style.display = 'flex');
    if (roles.includes('VALID_FINANCIER')) document.querySelectorAll('.role-financier').forEach(el => el.style.display = 'flex');
}

function setupInteractions() {
    document.getElementById('logout-btn').onclick = () => keycloak.logout();

    document.querySelectorAll('.nav-item').forEach(btn => {
        btn.onclick = () => {
            const role = btn.dataset.role;
            const endpoint = getEndpoint(role);
            
            // UI Update
            document.querySelectorAll('.nav-item').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            document.getElementById('page-title').innerText = btn.innerText;
            document.getElementById('api-endpoint').innerText = `GET ${endpoint}`;

            fetchFromAPI(role, endpoint);
        };
    });

    // Token Expiry Timer
    setInterval(() => {
        const timeLeft = Math.round(keycloak.tokenParsed.exp + keycloak.timeSkew - Date.now() / 1000);
        document.getElementById('token-expiry').innerText = timeLeft > 0 ? `${timeLeft}s` : 'Expiré';
        
        if (timeLeft < 30) {
            keycloak.updateToken(70).then(refreshed => {
                if (refreshed) console.log('Token rafraîchi');
            });
        }
    }, 1000);
}

function getEndpoint(role) {
    switch(role) {
        case 'public': return '/public/hello';
        case 'private': return '/private/hello';
        case 'agent': return '/agent/dashboard';
        case 'admin': return '/admin/dashboard';
        case 'juridique': return '/validation/juridique';
        case 'financier': return '/validation/financier';
        default: return '/public/hello';
    }
}

async function fetchFromAPI(type, endpoint) {
    const responseBox = document.getElementById('api-response');
    responseBox.innerText = 'Appel de l\'API en cours...';
    responseBox.style.color = '#38bdf8';

    try {
        const headers = {};
        if (type !== 'public') {
            headers['Authorization'] = `Bearer ${keycloak.token}`;
        }

        const response = await fetch(`${API_BASE}${endpoint}`, { headers });
        
        if (response.ok) {
            const data = await response.text();
            responseBox.innerText = `Success (200 OK)\n\n${data}`;
            responseBox.style.color = '#10b981';
        } else {
            const errorText = await response.text();
            responseBox.innerText = `Erreur ${response.status}\n\n${response.statusText}\n${errorText}`;
            responseBox.style.color = '#ef4444';
        }
    } catch (error) {
        responseBox.innerText = `Erreur Réseau\n\n${error.message}\n\nVérifiez que le serveur Spring Boot est lancé sur le port 8097.`;
        responseBox.style.color = '#ef4444';
    }
}

init();
