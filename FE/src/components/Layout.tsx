import { useState } from 'react';
import { Link, NavLink, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

export function Layout() {
  const { session, signOut } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);
  const location = useLocation();
  const close = () => setMenuOpen(false);

  return (
    <div className="app">
      <header className="header">
        <div className="header__inner container">
          <Link to="/" className="brand" onClick={close}>
            <img src="/pokeball.svg" alt="" width={28} height={28} />
            <span>Pokédex</span>
          </Link>

          <button
            type="button"
            className="header__toggle"
            aria-expanded={menuOpen}
            aria-controls="main-nav"
            onClick={() => setMenuOpen((open) => !open)}
          >
            <span className="sr-only">Menu</span>☰
          </button>

          <nav id="main-nav" className={`nav ${menuOpen ? 'nav--open' : ''}`}>
            <NavLink to="/" end onClick={close}>
              Browse
            </NavLink>
            <NavLink to="/collection" onClick={close}>
              My collection
            </NavLink>
            <div className="nav__account">
              {session ? (
                <>
                  <span className="nav__user" title="Signed in">
                    {session.username}
                  </span>
                  <button
                    type="button"
                    className="button button--ghost button--small"
                    onClick={() => {
                      signOut();
                      close();
                    }}
                  >
                    Sign out
                  </button>
                </>
              ) : (
                <Link
                  to="/sign-in"
                  state={{ from: location.pathname + location.search }}
                  className="button button--small"
                  onClick={close}
                >
                  Sign in
                </Link>
              )}
            </div>
          </nav>
        </div>
      </header>

      <main className="container main">
        <Outlet />
      </main>

      <footer className="footer container">
        Data from <a href="https://pokeapi.co" target="_blank" rel="noreferrer">PokeAPI</a>. Weights in kg
        (PokeAPI reports hectograms).
      </footer>
    </div>
  );
}
