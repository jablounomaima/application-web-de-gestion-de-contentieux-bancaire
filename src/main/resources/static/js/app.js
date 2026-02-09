// ============================================
// ROUTER - Redirection selon le rôle
// ============================================

const ROLE_DASHBOARDS = {
    'ROLE_ADMIN': '/dashboards/admin.html',
    'ROLE_AGENT_BANCAIRE': '/dashboards/agent.html',
    'ROLE_VALIDATEUR_JURIDIQUE': '/dashboards/juridique.html',
    'ROLE_AVOCAT': '/dashboards/avocat.html',
    'ROLE_HUISSIER': '/dashboards/huissier.html',
    'ROLE_EXPERT': '/dashboards/expert.html',
    'ROLE_VALIDATEUR_FINANCIER': '/dashboards/financier.html'
};

function decodeToken(token) {
    try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        return payload;
    } catch (e) {
        console.error('Invalid token', e);
        return null;
    }
}

function getUserRole() {
    const token = localStorage.getItem('token');
    if (!token) return null;
    
    const payload = decodeToken(token);
    if (!payload || !payload.sub) return null;
    
    // Faire un appel API pour obtenir les infos complètes de l'utilisateur
    return fetch('/api/auth/me', {
        headers: { 'Authorization': 'Bearer ' + token }
    })
    .then(res => res.json())
    .then(data => data.role)
    .catch(() => null);
}

function redirectToDashboard() {
    getUserRole().then(role => {
        if (!role) {
            window.location.href = '/login.html';
            return;
        }
        
        const dashboard = ROLE_DASHBOARDS[role];
        if (dashboard && !window.location.pathname.includes(dashboard)) {
            window.location.href = dashboard;
        }
    });
}

function logout() {
    localStorage.removeItem('token');
    window.location.href = '/login.html';
}

// Protection des pages
function requireAuth() {
    const token = localStorage.getItem('token');
    if (!token) {
        window.location.href = '/login.html';
        return false;
    }
    return true;
}

// API Helper
async function apiCall(endpoint, options = {}) {
    const token = localStorage.getItem('token');
    const defaultOptions = {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': token ? 'Bearer ' + token : ''
        }
    };
    
    const response = await fetch(endpoint, { ...defaultOptions, ...options });
    
    if (response.status === 401) {
        logout();
        throw new Error('Unauthorized');
    }
    
    return response;
}

// Notification system
function showNotification(message, type = 'info') {
    const notification = document.createElement('div');
    notification.className = `notification notification-${type}`;
    notification.innerHTML = `
        <div style="display: flex; align-items: center; gap: 1rem;">
            <span style="font-size: 1.5rem;">
                ${type === 'success' ? '✓' : type === 'error' ? '✗' : 'ℹ'}
            </span>
            <p>${message}</p>
        </div>
    `;
    
    document.body.appendChild(notification);
    
    setTimeout(() => {
        notification.style.animation = 'slideOut 0.3s ease';
        setTimeout(() => notification.remove(), 300);
    }, 3000);
}

// Format date
function formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleDateString('fr-FR', {
        year: 'numeric',
        month: 'long',
        day: 'numeric'
    });
}

// Format currency
function formatCurrency(amount) {
    return new Intl.NumberFormat('fr-FR', {
        style: 'currency',
        currency: 'TND'
    }).format(amount);
}
