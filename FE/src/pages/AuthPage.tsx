import { useState, type FormEvent } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';

const DEMO = { username: 'demo', password: 'demo1234' };

type Mode = 'sign-in' | 'register';

/** US-05: sign in, or register and be signed in straight away. */
export function AuthPage({ mode }: { mode: Mode }) {
  const { session, endedReason, signIn, register } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const from = (location.state as { from?: string } | null)?.from ?? '/collection';

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState(false);

  if (session && !submitting) return <Navigate to={from} replace />;

  const isRegister = mode === 'register';

  const submit = async (credentials: { username: string; password: string }) => {
    setError(null);
    setFieldErrors({});
    if (isRegister) {
      const local: Record<string, string> = {};
      if (credentials.username.length < 3 || credentials.username.length > 30) local.username = 'Use 3 to 30 characters.';
      if (credentials.password.length < 8) local.password = 'Use at least 8 characters.';
      if (Object.keys(local).length > 0) return setFieldErrors(local);
    }

    setSubmitting(true);
    try {
      await (isRegister ? register(credentials) : signIn(credentials));
      navigate(from, { replace: true });
    } catch (caught) {
      if (caught instanceof ApiError && caught.invalidFields.length > 0) {
        setFieldErrors(Object.fromEntries(caught.invalidFields.map(({ field, reason }) => [field, reason])));
      } else {
        setError(caught instanceof Error ? caught.message : 'Something went wrong.');
      }
    } finally {
      setSubmitting(false);
    }
  };

  const onSubmit = (event: FormEvent) => {
    event.preventDefault();
    void submit({ username: username.trim(), password });
  };

  return (
    <section className="auth">
      <h1>{isRegister ? 'Create an account' : 'Sign in'}</h1>
      <p className="muted">
        {isRegister
          ? 'An account lets you save Pokémon to your collection and edit them.'
          : 'Sign in to save, edit and remove Pokémon in your collection.'}
      </p>

      {endedReason && !error && <p className="banner">{endedReason}</p>}
      {error && (
        <p className="form__error" role="alert">
          {error}
        </p>
      )}

      <form className="form" onSubmit={onSubmit} noValidate>
        <div className="field">
          <label htmlFor="username">Username</label>
          <input
            id="username"
            autoComplete="username"
            value={username}
            aria-invalid={!!fieldErrors.username}
            onChange={(event) => setUsername(event.target.value)}
            required
          />
          {fieldErrors.username && <p className="field__error">{fieldErrors.username}</p>}
        </div>
        <div className="field">
          <label htmlFor="password">Password</label>
          <input
            id="password"
            type="password"
            autoComplete={isRegister ? 'new-password' : 'current-password'}
            value={password}
            aria-invalid={!!fieldErrors.password}
            onChange={(event) => setPassword(event.target.value)}
            required
          />
          {fieldErrors.password && <p className="field__error">{fieldErrors.password}</p>}
          {isRegister && !fieldErrors.password && <p className="field__hint">At least 8 characters.</p>}
        </div>
        <button type="submit" className="button button--block" disabled={submitting}>
          {submitting ? 'Please wait…' : isRegister ? 'Create account' : 'Sign in'}
        </button>
      </form>

      {!isRegister && (
        <div className="demo">
          <p className="small muted">
            Just looking? Use the demo account <code>{DEMO.username}</code> / <code>{DEMO.password}</code>.
          </p>
          <button type="button" className="button button--ghost button--block" disabled={submitting} onClick={() => void submit(DEMO)}>
            Sign in as demo
          </button>
        </div>
      )}

      <p className="auth__switch small">
        {isRegister ? (
          <>
            Already have an account?{' '}
            <Link to="/sign-in" state={location.state}>
              Sign in
            </Link>
          </>
        ) : (
          <>
            New here?{' '}
            <Link to="/register" state={location.state}>
              Create an account
            </Link>
          </>
        )}
      </p>
    </section>
  );
}
