const STORAGE_KEY = 'pokemon-interface.token';

export interface Session {
  token: string;
  username: string;
  /** Epoch milliseconds after which the backend rejects the token. */
  expiresAt: number;
}

/** Reads the subject and expiry from the JWT payload. The signature is the backend's concern, not ours. */
export function sessionFrom(token: string): Session | null {
  try {
    const payload = token.split('.')[1];
    const json = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
    const claims = JSON.parse(json) as { sub?: string; exp?: number };
    if (!claims.sub || !claims.exp) return null;
    return { token, username: claims.sub, expiresAt: claims.exp * 1000 };
  } catch {
    return null;
  }
}

export function isLive(session: Session | null): session is Session {
  return session !== null && session.expiresAt > Date.now();
}

export function loadToken(): string | null {
  try {
    return window.localStorage.getItem(STORAGE_KEY);
  } catch {
    return null;
  }
}

export function saveToken(token: string | null) {
  try {
    if (token) window.localStorage.setItem(STORAGE_KEY, token);
    else window.localStorage.removeItem(STORAGE_KEY);
  } catch {
    // Storage can be unavailable (private mode); the session then lasts for this tab only.
  }
}
