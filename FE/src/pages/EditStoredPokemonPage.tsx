import { Link, useNavigate, useParams } from 'react-router-dom';
import { PokemonSprite } from '../components/PokemonSprite';
import { EmptyState, ErrorState, Spinner } from '../components/Status';
import { StoredPokemonForm } from '../components/StoredPokemonForm';
import { useToast } from '../components/Toast';
import { dexNumber, displayName } from '../format';
import { useEditStoredPokemon, useStoredPokemon } from '../hooks/useStoredPokemon';
import { NotFoundPage } from './NotFoundPage';

/** US-04: edit our own fields of a stored Pokémon. PokeAPI's fields are read-only here. */
export function EditStoredPokemonPage() {
  const id = Number(useParams().id);
  if (!Number.isInteger(id) || id <= 0) return <NotFoundPage />;
  return <EditStoredPokemon id={id} />;
}

function EditStoredPokemon({ id }: { id: number }) {
  const navigate = useNavigate();
  const { notify } = useToast();
  const { data: pokemon, error, isPending, refetch } = useStoredPokemon(id);
  const edit = useEditStoredPokemon(id);

  if (isPending) return <Spinner />;
  if (error) return <ErrorState error={error} onRetry={() => refetch()} />;
  if (!pokemon) {
    return (
      <EmptyState title="Not in your collection">
        <p className="muted">
          Pokémon {dexNumber(id)} isn't stored locally. <Link to={`/pokemon/${id}`}>View it</Link> and save it first.
        </p>
      </EmptyState>
    );
  }

  const name = displayName(pokemon.name);

  return (
    <section className="narrow">
      <header className="edit-header">
        <PokemonSprite src={pokemon.sprite} alt={name} size={80} />
        <div>
          <span className="card__number">{dexNumber(pokemon.id)}</span>
          <h1>Edit {name}</h1>
          <p className="muted small">
            {pokemon.category ?? 'Unknown category'}. Name, category, weight and sprite come from PokeAPI and can't be edited.
          </p>
        </div>
      </header>

      <StoredPokemonForm
        pokemon={pokemon}
        submitting={edit.isPending}
        serverError={edit.error}
        onCancel={() => navigate(-1)}
        onSubmit={(fields) =>
          edit.mutate(fields, {
            onSuccess: () => {
              notify(`${name} updated.`);
              navigate('/collection');
            },
          })
        }
      />
    </section>
  );
}
