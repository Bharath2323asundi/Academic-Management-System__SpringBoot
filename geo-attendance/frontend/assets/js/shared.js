/* ============================================
   Geo-Attendance - Shared JS Utilities
   ============================================ */

// Auto-detect backend URL or set a production one (e.g. 'https://my-backend.onrender.com/api')
const isLocal = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1';
const API_BASE = isLocal ? 'http://localhost:8080/api' : 'https://your-production-backend-url.com/api';

// ── Token helpers ──────────────────────────────────────────────────────
const Auth = {
  setToken(token) { localStorage.setItem('ga_token', token); },
  getToken()      { return localStorage.getItem('ga_token'); },
  setUser(user)   { localStorage.setItem('ga_user', JSON.stringify(user)); },
  getUser()       {
    try { return JSON.parse(localStorage.getItem('ga_user')); }
    catch { return null; }
  },
  getRole()       { const u = Auth.getUser(); return u ? u.role : null; },
  clear()         { localStorage.removeItem('ga_token'); localStorage.removeItem('ga_user'); },
  isLoggedIn()    { return !!Auth.getToken(); },

  requireAuth(expectedRole) {
    if (!Auth.isLoggedIn()) {
      window.location.href = '/frontend/index.html';
      return false;
    }
    if (expectedRole && Auth.getRole() !== expectedRole) {
      Auth.clear();
      window.location.href = '/frontend/index.html';
      return false;
    }
    return true;
  }
};

// ── API helper ─────────────────────────────────────────────────────────
async function apiRequest(method, endpoint, body = null, requiresAuth = true) {
  const headers = { 'Content-Type': 'application/json' };
  if (requiresAuth) {
    const token = Auth.getToken();
    if (token) headers['Authorization'] = `Bearer ${token}`;
  }

  const options = { method, headers };
  if (body) options.body = JSON.stringify(body);

  try {
    const res = await fetch(`${API_BASE}${endpoint}`, options);

    if (res.status === 401 || res.status === 403) {
      Auth.clear();
      window.location.href = '/frontend/index.html';
      return null;
    }

    const data = await res.json().catch(() => ({}));
    return { ok: res.ok, status: res.status, data };
  } catch (err) {
    console.error('API Error:', err);
    return { ok: false, status: 0, data: { message: 'Network error. Is the server running?' } };
  }
}

// ── Toast Notifications ────────────────────────────────────────────────
function showToast(message, type = 'info', duration = 3500) {
  let container = document.getElementById('toast-container');
  if (!container) {
    container = document.createElement('div');
    container.id = 'toast-container';
    container.style.cssText = `
      position: fixed; bottom: 1.5rem; right: 1.5rem;
      z-index: 9999; display: flex; flex-direction: column; gap: 8px;
      max-width: 340px;
    `;
    document.body.appendChild(container);
  }

  const colors = {
    success: '#27ae60', danger: '#e74c3c',
    warning: '#f39c12', info: '#1b6ca8'
  };
  const icons = {
    success: '✓', danger: '✕', warning: '⚠', info: 'ℹ'
  };

  const toast = document.createElement('div');
  toast.style.cssText = `
    background: #fff;
    border-left: 4px solid ${colors[type] || colors.info};
    padding: 12px 16px;
    border-radius: 10px;
    box-shadow: 0 4px 20px rgba(0,0,0,0.14);
    display: flex; align-items: flex-start; gap: 10px;
    font-family: 'DM Sans', sans-serif;
    font-size: 0.88rem;
    color: #1a2332;
    transform: translateX(120%);
    transition: transform 0.3s ease;
    line-height: 1.45;
    cursor: pointer;
  `;

  toast.innerHTML = `
    <span style="color:${colors[type]};font-weight:700;font-size:1rem;line-height:1.2;">${icons[type]}</span>
    <span>${message}</span>
  `;

  toast.onclick = () => removeToast(toast);
  container.appendChild(toast);

  requestAnimationFrame(() => {
    requestAnimationFrame(() => { toast.style.transform = 'translateX(0)'; });
  });

  setTimeout(() => removeToast(toast), duration);
}

function removeToast(toast) {
  toast.style.transform = 'translateX(120%)';
  setTimeout(() => toast.remove(), 300);
}

// ── Date/Time helpers ──────────────────────────────────────────────────
function formatDate(dateStr) {
  if (!dateStr) return '—';
  const d = new Date(dateStr);
  return d.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
}

function formatTime(timeStr) {
  if (!timeStr) return '—';
  const [h, m] = timeStr.split(':');
  const hour = parseInt(h);
  const ampm = hour >= 12 ? 'PM' : 'AM';
  const h12 = hour % 12 || 12;
  return `${h12}:${m} ${ampm}`;
}

function today() {
  return new Date().toISOString().split('T')[0];
}

// ── Set nav user info ──────────────────────────────────────────────────
function initNavbar(logoutPath = '/frontend/index.html') {
  const user = Auth.getUser();
  if (!user) return;

  const nameEl = document.getElementById('nav-username');
  const avatarEl = document.getElementById('nav-avatar');

  if (nameEl) nameEl.textContent = user.name || user.username || 'User';
  if (avatarEl) avatarEl.textContent = (user.name || user.username || 'U')[0].toUpperCase();

  const logoutBtn = document.getElementById('logout-btn');
  if (logoutBtn) {
    logoutBtn.addEventListener('click', (e) => {
      e.preventDefault();
      Auth.clear();
      window.location.href = logoutPath;
    });
  }
}

// ── Haversine (client-side, for display only) ──────────────────────────
function haversineDistance(lat1, lon1, lat2, lon2) {
  const R = 6371000;
  const dLat = (lat2 - lat1) * Math.PI / 180;
  const dLon = (lon2 - lon1) * Math.PI / 180;
  const a = Math.sin(dLat/2)**2
          + Math.cos(lat1 * Math.PI/180) * Math.cos(lat2 * Math.PI/180)
          * Math.sin(dLon/2)**2;
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
}

// ── Loading button state ───────────────────────────────────────────────
function setLoading(btn, loading, text = '') {
  if (loading) {
    btn.dataset.original = btn.innerHTML;
    btn.innerHTML = `<span class="spinner"></span> ${text || 'Loading...'}`;
    btn.disabled = true;
  } else {
    btn.innerHTML = btn.dataset.original || text;
    btn.disabled = false;
  }
}
