import { get, writable } from 'svelte/store';
import { goto } from '$app/navigation';

export const token = writable<string | null>(null);
export const isAuthenticated = writable<boolean>(false);
export const role = writable<string | null>(null);
let API_BASE_URL = "http://localhost:8080/";

export async function setAuth(newToken: string | null): Promise<void> {
    token.set(newToken);
    isAuthenticated.set(!!newToken);
    if (newToken) {
        try {
            const res = await apiFetch('/account');
            if (res.ok) {
                const data = await res.json();
                role.set(data.role);
                console.log(data.role)
            } else {
                console.error("Nie udało się pobrać danych konta przy logowaniu.");
                logout();
            }
        } catch (error) {
            console.error("Błąd podczas pobierania roli użytkownika:", error);
            logout();
        }
    } else {
        role.set(null);
    }
}

export function logout(): void {
    token.set(null);
    isAuthenticated.set(false);
    role.set(null);
    goto("/account")
}

async function refreshToken(): Promise<string | null> {
    try {
        const res = await fetch(`${API_BASE_URL}/auth/refresh`, {
            method: 'POST',
            credentials: 'include', 
            headers: {
                'Content-Type': 'application/json'
            }
        });

        if (res.ok) {
            const data = await res.json();
            const newJwtToken = data.token; 
            
            await setAuth(newJwtToken);
            return newJwtToken;
        } else {
            console.warn("Refresh token expired or invalid.");
            logout();
            return null;
        }
    } catch (error) {
        console.error("Error refreshing token:", error);
        logout();
        return null;
    }
}

export async function apiFetch(endpoint: string, options: RequestInit = {}): Promise<Response> {
    
    const headers = new Headers(options.headers);
    if (!headers.has('Content-Type')) {
        headers.set('Content-Type', 'application/json');
    }
    
    let currentToken = get(token);
    if (currentToken) {
        headers.set('Authorization', `Bearer ${currentToken}`);
    }

    const fetchOptions: RequestInit = {
        ...options,
        headers,
        credentials: options.credentials || 'include' 
    };

    let response = await fetch(`${API_BASE_URL}${endpoint}`, fetchOptions);

    if (response.status === 401) {
        console.log("Token expired, attempting to refresh...");
        
        // Spróbuj pobrać nowy token
        const newToken = await refreshToken();

        if (newToken) {
            const newHeaders = {
                ...headers,
                'Authorization': `Bearer ${newToken}`
            };
            
            fetchOptions.headers = newHeaders;
            response = await fetch(`${API_BASE_URL}${endpoint}`, fetchOptions);
        }
    }

    return response;
}