// store authentication credentials
import { writable } from 'svelte/store';

export const token = writable<string | null>(null);
export const isAuthenticated = writable<boolean>(false);

export function setAuth(newToken: string | null): void {
    token.set(newToken);
    isAuthenticated.set(!!newToken);
}