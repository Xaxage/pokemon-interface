import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { displayName } from '../format';
import { useSynchronisePokemon } from '../hooks/useStoredPokemon';
import { useToast } from './Toast';

interface SyncButtonProps {
  id: number;
  name: string;
  /** Whether the Pokémon is already in the collection, so syncing refreshes it rather than adding it. */
  stored?: boolean;
  className?: string;
}

/** Copies a Pokémon from PokeAPI into the collection, or refreshes the copy already there. Needs a signed-in user. */
export function SyncButton({ id, name, stored = false, className = '' }: SyncButtonProps) {
  const { session } = useAuth();
  const location = useLocation();
  const { notify } = useToast();
  const sync = useSynchronisePokemon();

  if (!session) {
    return (
      <Link to="/sign-in" state={{ from: location.pathname + location.search }} className={`button button--ghost button--small ${className}`}>
        Sign in to collect
      </Link>
    );
  }

  const onClick = () =>
    sync.mutate(id, {
      onSuccess: () =>
        notify(stored ? `${displayName(name)} refreshed from PokeAPI.` : `${displayName(name)} saved to your collection.`),
      onError: (error) => notify(error.message, 'error'),
    });

  return (
    <button type="button" className={`button button--small ${stored ? 'button--ghost' : ''} ${className}`} onClick={onClick} disabled={sync.isPending}>
      {sync.isPending ? 'Saving…' : stored ? '↻ Refresh from PokeAPI' : '+ Save to collection'}
    </button>
  );
}
