import { Link } from 'react-router-dom';
import type { Pokemon } from '../api/types';
import { dexNumber, displayName, weightInKg } from '../format';
import { usePrefetchPokemonDetails } from '../hooks/usePokemon';
import { PokemonSprite } from './PokemonSprite';
import { SkillList } from './SkillList';
import { SyncButton } from './SyncButton';

export function PokemonCard({ pokemon }: { pokemon: Pokemon }) {
  const name = displayName(pokemon.name);
  const prefetchDetails = usePrefetchPokemonDetails();
  const prefetch = () => prefetchDetails(pokemon.id);

  return (
    <li className="card">
      <Link to={`/pokemon/${pokemon.id}`} className="card__link" onMouseEnter={prefetch} onFocus={prefetch}>
        <PokemonSprite src={pokemon.sprite} alt={name} />
        <span className="card__number">{dexNumber(pokemon.id)}</span>
        <h3 className="card__title">{name}</h3>
      </Link>
      <dl className="facts">
        <div>
          <dt>Category</dt>
          <dd>{pokemon.category ?? '—'}</dd>
        </div>
        <div>
          <dt>Weight</dt>
          <dd>{weightInKg(pokemon.weight)}</dd>
        </div>
      </dl>
      <SkillList skills={pokemon.skills} />
      <div className="card__actions">
        <SyncButton id={pokemon.id} name={pokemon.name} />
      </div>
    </li>
  );
}
