// ============================================
// Geo-Attendance - Shared JS Utilities
// ============================================

const API_BASE = 'http://localhost:8080/api';

// ─── Auth Helpers ──────────────────────────────
const Auth = {
  getToken: () => localStorage.getItem('token'),
  getRole:  () => localStorage.getItem('role'),
  getUser:  () => JSON.parse(localStorage.getItem('user') || '{}'),

  setSession(data) {
    localStorage.setItem('token', data.token);
    localStorage.setItem('role', data.role);
    localStorage.setItem('user', JSON.stringify({
      name: data.name,
      id: data.id,
      studentId: data.studentId || null
    }));
  },

  clear() {
    ['token', 'role', 'user'].forEach(k => localStorage.removeItem(k));
  },

  requireAuth(role) {
    const token = this.getToken();
    const userRole = this.getRole();
    if (!token) {
      window.location.href = role === 'ADMIN' ? '../admin/login.html' : '../student/login.html';
      return false;
    }
    if (role && userRole !== role) {
      this.clear();
      window.location.href = role === 'ADMIN' ? '../admin/login.html' : '../student/login.html';
      return false;
    }
    return true;
  }
};

// ─── API Helper ────────────────────────────────
const api = {
  async request(endpoint, options = {}) {
    const token = Auth.getToken();
    const headers = { 'Content-Type': 'application/json', ...options.headers };
    if (token) headers['Authorization'] = `Bearer ${token}`;

    try {
      const res = await fetch(`${API_BASE}${endpoint}`, {
        ...options,
        headers,
        body: options.body ? JSON.stringify(options.body) : undefined
      });

      const data = await res.json().catch(() => ({}));

      if (res.status === 401 || res.status === 403) {
        if (endpoint !== '/auth/admin/login' && endpoint !== '/auth/student/login') {
          Auth.clear();
          const role = Auth.getRole();
          window.location.href = role === 'ADMIN' ? '../admin/login.html' : '../student/login.html';
        }
      }

      return { ok: res.ok, status: res.status, data };
    } catch (err) {
      return { ok: false, status: 0, data: { message: 'Network error. Is the server running?' } };
    }
  },

  get:    (url)          => api.request(url),
  post:   (url, body)    => api.request(url, { method: 'POST', body }),
  put:    (url, body)    => api.request(url, { method: 'PUT', body }),
  delete: (url)          => api.request(url, { method: 'DELETE' })
};

// ─── UI Helpers ────────────────────────────────
const UI = {
  showAlert(el, message, type = 'danger') {
    const icons = { success: '✅', danger: '❌', warning: '⚠️', info: 'ℹ️' };
    el.className = `alert alert-${type}`;
    el.innerHTML = `<span>${icons[type]}</span><span>${message}</span>`;
    el.classList.remove('hidden');
    if (type === 'success') {
      setTimeout(() => el.classList.add('hidden'), 4000);
    }
  },

  hideAlert(el) { el.classList.add('hidden'); },

  setLoading(btn, loading) {
    if (loading) {
      btn.dataset.originalText = btn.innerHTML;
      btn.innerHTML = `<span class="spinner"></span> Processing...`;
      btn.disabled = true;
    } else {
      btn.innerHTML = btn.dataset.originalText || btn.innerHTML;
      btn.disabled = false;
    }
  },

  formatDate(dateStr) {
    const d = new Date(dateStr);
    return d.toLocaleDateString('en-IN', { year: 'numeric', month: 'short', day: '2-digit' });
  },

  formatTime(timeStr) {
    if (!timeStr) return '-';
    const [h, m] = timeStr.split(':');
    const ampm = +h >= 12 ? 'PM' : 'AM';
    const hour = +h % 12 || 12;
    return `${hour}:${m} ${ampm}`;
  },

  badgeStatus(status) {
    const map = {
      present: 'badge-success',
      absent: 'badge-danger',
      approved: 'badge-success',
      rejected: 'badge-danger',
      pending: 'badge-warning'
    };
    return `<span class="badge ${map[status] || 'badge-info'}">${status}</span>`;
  }
};

// ─── Geolocation ───────────────────────────────
const Geo = {
  getPosition() {
    return new Promise((resolve, reject) => {
      if (!navigator.geolocation) {
        reject(new Error('Geolocation is not supported by your browser'));
        return;
      }
      navigator.geolocation.getCurrentPosition(resolve, reject, {
        enableHighAccuracy: true,
        timeout: 15000,
        maximumAge: 0
      });
    });
  },

  haversineDistance(lat1, lon1, lat2, lon2) {
    const R = 6371000;
    const dLat = (lat2 - lat1) * Math.PI / 180;
    const dLon = (lon2 - lon1) * Math.PI / 180;
    const a = Math.sin(dLat / 2) ** 2
            + Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180)
            * Math.sin(dLon / 2) ** 2;
    return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  }
};

// ─── Navbar Builder ────────────────────────────
function buildNavbar(role) {
  const user = Auth.getUser();
  const adminLinks = `
    <li><a class="nav-link" href="dashboard.html">📊 Dashboard</a></li>
    <li><a class="nav-link" href="students.html">👥 Students</a></li>
    <li><a class="nav-link" href="attendance.html">📋 Attendance</a></li>
    <li><a class="nav-link" href="leaves.html">🏖️ Leaves</a></li>
    <li><a class="nav-link" href="settings.html">⚙️ Settings</a></li>`;
  const studentLinks = `
    <li><a class="nav-link" href="dashboard.html">🏠 Dashboard</a></li>
    <li><a class="nav-link" href="mark-attendance.html">📍 Mark Attendance</a></li>
    <li><a class="nav-link" href="history.html">📅 History</a></li>
    <li><a class="nav-link" href="leave.html">🏖️ Leaves</a></li>`;

  return `
    <nav class="navbar">
      <div class="navbar-brand">
        <div class="brand-icon">📍</div>
        <span>GeoAttend</span>
      </div>
      <ul class="navbar-nav">
        ${role === 'ADMIN' ? adminLinks : studentLinks}
        <li>
          <span class="nav-link" style="color:rgba(255,255,255,0.7);font-size:0.85rem;">
            👤 ${user.name || ''}
          </span>
        </li>
        <li><a class="nav-link logout" id="logout-btn">🚪 Logout</a></li>
      </ul>
    </nav>`;
}

function buildFooter() {
  return `
    <footer class="footer">
      <span>© ${new Date().getFullYear()}</span>
      <span>GeoAttend — Geo-Fencing Attendance System</span>
      <span>Built with <span>♥</span></span>
    </footer>`;
}

function initPage(role) {
  if (!Auth.requireAuth(role)) return false;

  // Set active nav link
  const currentPage = window.location.pathname.split('/').pop();
  document.querySelectorAll('.nav-link').forEach(link => {
    if (link.getAttribute('href') === currentPage) link.classList.add('active');
  });

  // Logout handler
  const logoutBtn = document.getElementById('logout-btn');
  if (logoutBtn) {
    logoutBtn.addEventListener('click', () => {
      Auth.clear();
      window.location.href = role === 'ADMIN' ? 'login.html' : '../student/login.html';
    });
  }

  return true;
}
