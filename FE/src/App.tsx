import { Route, Routes } from 'react-router-dom';
import { Layout } from './components/Layout';
import { RequireAuth } from './components/RequireAuth';
import { AuthPage } from './pages/AuthPage';
import { BrowsePage } from './pages/BrowsePage';
import { CollectionPage } from './pages/CollectionPage';
import { EditStoredPokemonPage } from './pages/EditStoredPokemonPage';
import { NotFoundPage } from './pages/NotFoundPage';
import { PokemonDetailPage } from './pages/PokemonDetailPage';

export function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<BrowsePage />} />
        <Route path="pokemon/:id" element={<PokemonDetailPage />} />
        <Route path="collection" element={<CollectionPage />} />
        <Route
          path="collection/:id/edit"
          element={
            <RequireAuth>
              <EditStoredPokemonPage />
            </RequireAuth>
          }
        />
        <Route path="sign-in" element={<AuthPage key="sign-in" mode="sign-in" />} />
        <Route path="register" element={<AuthPage key="register" mode="register" />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}
