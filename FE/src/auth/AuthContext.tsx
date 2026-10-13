import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react';
import { authApi } from '../api/auth';
import { configureClient } from '../api/client';
import type { Credentials } from '../api/types';
import { isLive, loadToken, saveToken, sessionFrom, type Session } from './token';

const EXPIRED = 'Your session has expired. Please sign in again.';

interface AuthState {
  session: Session | null;
  /** Set when the session ended on its own (expired or rejected), so the UI can say why. */
  endedReason: string | null;
  signIn: (credentials: Credentials) => Promise<void>;
  register: (credentials: Credentials) => Promise<void>;
  signOut: () => void;
}

const AuthContext = createContext<AuthState | null>(null);

function restoreSession(): Session | null {
  const stored = loadToken();
  const session = stored ? sessionFrom(stored) : null;
  if (isLive(session)) return session;
  saveToken(null);
  return null;
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(restoreSession);
  const [endedReason, setEndedReason] = useState<string | null>(null);
  const sessionRef = useRef(session);
  sessionRef.current = session;

  const end = useCallback((reason: string | null) => {
    saveToken(null);
    setSession(null);
    setEndedReason(reason);
  }, []);

  // Configured during render (not in an effect) so the very first requests already carry the token.
  configureClient({
    token: () => sessionRef.current?.token ?? null,
    onUnauthorized: () => end(EXPIRED),
  });

  // Ends the session the moment the token expires, rather than on the next failed request.
  useEffect(() => {
    if (!session) return;
    const timer = window.setTimeout(() => end(EXPIRED), Math.max(session.expiresAt - Date.now(), 0));
    return () => window.clearTimeout(timer);
  }, [session, end]);

  const signIn = useCallback(async (credentials: Credentials) => {
    const { token } = await authApi.signIn(credentials);
    const next = sessionFrom(token);
    if (!next) throw new Error('The server returned a token this app cannot read.');
    saveToken(token);
    setEndedReason(null);
    setSession(next);
  }, []);

  const register = useCallback(
    async (credentials: Credentials) => {
      await authApi.register(credentials);
      await signIn(credentials);
    },
    [signIn],
  );

  const signOut = useCallback(() => end(null), [end]);

  const value = useMemo(
    () => ({ session, endedReason, signIn, register, signOut }),
    [session, endedReason, signIn, register, signOut],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used inside <AuthProvider>');
  return context;
}
