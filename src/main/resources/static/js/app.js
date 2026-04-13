const API_URL = "/api";

const app = {
    // Auth State
    user: JSON.parse(localStorage.getItem('user')) || null,
    token: localStorage.getItem('token') || null,

    init() {
        this.updateUI();
    },

    saveAuth(userData, token) {
        this.user = userData;
        this.token = token;
        localStorage.setItem('user', JSON.stringify(userData));
        localStorage.setItem('token', token);
    },

    logout() {
        localStorage.removeItem('user');
        localStorage.removeItem('token');
        window.location.href = 'index.html';
    },

    getHeaders() {
        return {
            'Content-Type': 'application/json',
            'Authorization': this.token ? `Bearer ${this.token}` : ''
        };
    },

    async request(endpoint, options = {}) {
        const url = `${API_URL}${endpoint}`;
        const headers = this.getHeaders();
        
        console.log(`[API] Fetching: ${url}`, options);
        
        try {
            const response = await fetch(url, { ...options, headers });
            const contentType = response.headers.get("content-type");
            
            console.log(`[API] Response: ${response.status} ${response.statusText}`, { contentType });

            if (contentType && contentType.includes("application/pdf")) {
                return await response.blob();
            }

            let data;
            if (contentType && contentType.includes("application/json")) {
                const text = await response.text();
                try {
                    data = text ? JSON.parse(text) : {};
                } catch (e) {
                    console.error("[API] JSON Parse Error. Raw text:", text);
                    data = { message: text };
                }
            } else {
                data = { message: await response.text() };
            }
            
            if (!response.ok) {
                // If it was a blob but failed, let's try to parse the error message if possible
                if (data instanceof Blob) {
                    const errorText = await data.text();
                    throw new Error(errorText || `Error ${response.status}`);
                }
                throw new Error(data.message || `Error ${response.status}: ${response.statusText}`);
            }
            return data;
        } catch (error) {
            console.error("[API] Request Failure:", error);
            this.showToast(error.message, 'danger');
            throw error;
        }
    },

    showToast(message, type = 'success') {
        const toast = document.getElementById('toast');
        if (!toast) return;
        
        toast.innerText = message;
        toast.style.borderLeftColor = type === 'success' ? 'var(--accent)' : 'var(--secondary)';
        toast.classList.add('show');
        
        setTimeout(() => {
            toast.classList.remove('show');
        }, 3000);
    },

    updateUI() {
        // Handle common UI elements across pages (e.g., username in navbar)
        const usernameEl = document.getElementById('nav-username');
        if (usernameEl && this.user) {
            usernameEl.innerText = this.user.name;
        }
    }
};

// Initialize on load
document.addEventListener('DOMContentLoaded', () => app.init());
