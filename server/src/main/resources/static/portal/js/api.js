const API_BASE = window.location.origin; // same server — no CORS needed

function getToken() { return localStorage.getItem('vd_token'); }
function getProfile() {
    const raw = localStorage.getItem('vd_profile');
    return raw ? JSON.parse(raw) : null;
}
function saveSession(token, profile) {
    localStorage.setItem('vd_token', token);
    localStorage.setItem('vd_profile', JSON.stringify(profile));
}
function clearSession() {
    localStorage.removeItem('vd_token');
    localStorage.removeItem('vd_profile');
}
function requireLogin() {
    if (!getToken()) window.location.href = '/portal/login.html';
}

async function apiFetch(path, options = {}) {
    const headers = Object.assign(
        { 'Authorization': 'Bearer ' + getToken() },
        options.headers || {}
    );
    if (options.body) headers['Content-Type'] = 'application/json';
    const resp = await fetch(API_BASE + path, Object.assign({}, options, { headers }));
    if (resp.status === 401) {
        clearSession();
        window.location.href = '/portal/login.html';
        throw new Error('Not authenticated');
    }
    return resp;
}

async function checkMaintenanceBanner() {
    try {
        const resp = await fetch(API_BASE + '/api/system-status');
        const data = await resp.json();
        const existing = document.getElementById('maintBanner');
        if (data.minutesRemaining >= 0) {
            const text = `⚠ Scheduled maintenance in ${data.minutesRemaining} min: ${data.message}`;
            if (existing) { existing.textContent = text; return; }
            const banner = document.createElement('div');
            banner.id = 'maintBanner';
            banner.textContent = text;
            banner.style.cssText = 'background:#d29922;color:#1e2b38;padding:8px 16px;font-size:12px;font-weight:600;text-align:center;';
            document.body.prepend(banner);
        } else if (existing) {
            existing.remove();
        }
    } catch (e) { /* ignore — this is a non-critical banner */ }
}
checkMaintenanceBanner();
setInterval(checkMaintenanceBanner, 60000);

const api = {
    get: (path) => apiFetch(path, { method: 'GET' }),
    post: (path, body) => apiFetch(path, { method: 'POST', body: JSON.stringify(body) }),
    put: (path, body) => apiFetch(path, { method: 'PUT', body: body ? JSON.stringify(body) : undefined }),
    del: (path) => apiFetch(path, { method: 'DELETE' })
};