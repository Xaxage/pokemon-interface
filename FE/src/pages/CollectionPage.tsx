import { useEffect, useState, type FormEvent } from 'react';
import { Link, useLocation } from 'react-router-dom';
import type { StoredPokemon } from '../api/types';
import { useAuth } from '../auth/AuthContext';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { Pagination } from '../components/Pagination';
import { PokemonSprite } from '../components/PokemonSprite';
import { EmptyState, ErrorState, SkeletonGrid } from '../components/Status';
import { SyncButton } from '../components/SyncButton';
import { useToast } from '../components/Toast';
import { dexNumber, displayName, weightInKg } from '../format';
import { usePagination } from '../hooks/usePagination';
import { useDeleteStoredPokemon, useStoredPokemonPage, useSynchronisePokemon } from '../hooks/useStoredPokemon';

/** US-03 / US-04: the Pokémon stored locally, with our own fields, and the actions on them. */
export function CollectionPage() {
  const { session } = useAuth();
  const location = useLocation();
  const { notify } = useToast();
  const { page, size, setPage, setSize } = usePagination();
  const { data, error, isPending, isFetching, isPlaceholderData, refetch } = useStoredPokemonPage(page, size);
  const remove = useDeleteStoredPokemon();
  const [toDelete, setToDelete] = useState<StoredPokemon | null>(null);

  // After deleting the last Pokémon on the last page, step back to the page before it.
  useEffect(() => {
    if (data && data.content.length === 0 && page > 0 && page >= data.totalPages) setPage(Math.max(data.totalPages - 1, 0));
  }, [data, page, setPage]);

  const confirmDelete = () => {
    if (!toDelete) return;
    remove.mutate(toDelete.id, {
      onSuccess: () => notify(`${displayName(toDelete.name)} removed from your collection.`),
      onError: (deleteError) => notify(deleteError.message, 'error'),
      onSettled: () => setToDelete(null),
    });
  };

  return (
    <section>
      <header className="page-header">
        <div>
          <h1>My collection</h1>
          <p className="muted">Pokémon saved locally, with your own localized name, region and tags.</p>
        </div>
        {session && <AddByNumber />}
      </header>

      {!session && (
        <p className="banner">
          <Link to="/sign-in" state={{ from: location.pathname + location.search }}>
            Sign in
          </Link>{' '}
          to add, edit or remove Pokémon. Try the demo account: <code>demo</code> / <code>demo1234</code>.
        </p>
      )}

      {isPending ? (
        <SkeletonGrid count={6} />
      ) : error && !data ? (
        <ErrorState error={error} onRetry={() => refetch()} />
      ) : data.count === 0 ? (
        <EmptyState title="Your collection is empty">
          <p className="muted">
            <Link to="/">Browse Pokémon</Link> and save the ones you like.
          </p>
        </EmptyState>
      ) : (
        <>
          <Pagination
            page={page}
            size={size}
            totalPages={data.totalPages}
            count={data.count}
            busy={isFetching}
            onPageChange={setPage}
            onSizeChange={setSize}
          />
          <ul className={`grid grid--wide ${isPlaceholderData ? 'grid--stale' : ''}`}>
            {data.content.map((pokemon) => (
              <StoredPokemonCard
                key={pokemon.id}
                pokemon={pokemon}
                canModify={!!session}
                onDelete={() => setToDelete(pokemon)}
              />
            ))}
          </ul>
        </>
      )}

      <ConfirmDialog
        open={toDelete !== null}
        title="Remove from collection?"
        message={
          toDelete
            ? `${displayName(toDelete.name)} and your own fields for it will be deleted. You can save it again from PokeAPI later.`
            : ''
        }
        confirmLabel="Remove"
        busy={remove.isPending}
        onConfirm={confirmDelete}
        onCancel={() => setToDelete(null)}
      />
    </section>
  );
}

function StoredPokemonCard({
  pokemon,
  canModify,
  onDelete,
}: {
  pokemon: StoredPokemon;
  canModify: boolean;
  onDelete: () => void;
}) {
  const name = displayName(pokemon.name);

  return (
    <li className="card card--row">
      <Link to={`/pokemon/${pokemon.id}`} className="card__link">
        <PokemonSprite src={pokemon.sprite} alt={name} />
      </Link>
      <div className="card__body">
        <span className="card__number">{dexNumber(pokemon.id)}</span>
        <h3 className="card__title">
          <Link to={`/pokemon/${pokemon.id}`}>{name}</Link>
          {pokemon.localizedName && <span className="card__subtitle"> · {pokemon.localizedName}</span>}
        </h3>
        <dl className="facts">
          <div>
            <dt>Category</dt>
            <dd>{pokemon.category ?? '—'}</dd>
          </div>
          <div>
            <dt>Weight</dt>
            <dd>{weightInKg(pokemon.weight)}</dd>
          </div>
          <div>
            <dt>Region</dt>
            <dd>{pokemon.region ?? <span className="muted">—</span>}</dd>
          </div>
        </dl>
        {pokemon.tags.length > 0 && (
          <div className="tags">
            {pokemon.tags.map((tag) => (
              <span key={tag} className="tag">
                {tag}
              </span>
            ))}
          </div>
        )}
        {canModify && (
          <div className="card__actions">
            <Link to={`/collection/${pokemon.id}/edit`} className="button button--small">
              Edit
            </Link>
            <SyncButton id={pokemon.id} name={pokemon.name} stored />
            <button type="button" className="button button--small button--danger-ghost" onClick={onDelete}>
              Remove
            </button>
          </div>
        )}
      </div>
    </li>
  );
}

/** Saves a Pokémon straight from PokeAPI by its Pokédex number. */
function AddByNumber() {
  const [value, setValue] = useState('');
  const sync = useSynchronisePokemon();
  const { notify } = useToast();

  const onSubmit = (event: FormEvent) => {
    event.preventDefault();
    const id = Number(value);
    if (!Number.isInteger(id) || id <= 0) {
      notify('Enter a Pokédex number, e.g. 25.', 'error');
      return;
    }
    sync.mutate(id, {
      onSuccess: (pokemon) => {
        notify(`${displayName(pokemon.name)} saved to your collection.`);
        setValue('');
      },
      onError: (error) => notify(error.message, 'error'),
    });
  };

  return (
    <form className="inline-form" onSubmit={onSubmit}>
      <label htmlFor="add-id" className="sr-only">
        Pokédex number
      </label>
      <input
        id="add-id"
        type="number"
        min={1}
        inputMode="numeric"
        placeholder="Pokédex #, e.g. 25"
        value={value}
        onChange={(event) => setValue(event.target.value)}
      />
      <button type="submit" className="button" disabled={sync.isPending || value === ''}>
        {sync.isPending ? 'Saving…' : 'Add'}
      </button>
    </form>
  );
}
