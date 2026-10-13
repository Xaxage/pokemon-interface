import { Link, useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { EvolutionTree } from '../components/EvolutionTree';
import { InCollectionBadge } from '../components/InCollectionBadge';
import { PokemonSprite } from '../components/PokemonSprite';
import { SkillTable } from '../components/SkillList';
import { StatBars } from '../components/StatBars';
import { ErrorState, Spinner } from '../components/Status';
import { SyncButton } from '../components/SyncButton';
import { dexNumber, displayName, weightInKg } from '../format';
import { usePokemonDetails, usePokemonSkills } from '../hooks/usePokemon';
import { useStoredPokemon } from '../hooks/useStoredPokemon';
import { NotFoundPage } from './NotFoundPage';

/** US-02: one Pokémon in depth, plus where it stands in the reader's collection. */
export function PokemonDetailPage() {
  const id = Number(useParams().id);
  if (!Number.isInteger(id) || id <= 0) return <NotFoundPage />;
  return <PokemonDetail id={id} />;
}

function PokemonDetail({ id }: { id: number }) {
  const navigate = useNavigate();
  const { data: pokemon, error, isPending, refetch } = usePokemonDetails(id);

  if (isPending) return <Spinner label="Loading Pokémon…" />;
  if (error) return <ErrorState error={error} onRetry={() => refetch()} />;

  const name = displayName(pokemon.name);

  return (
    <article className="detail">
      <button type="button" className="back-link" onClick={() => navigate(-1)}>
        ← Back
      </button>

      <header className="detail__hero">
        <PokemonSprite src={pokemon.image} alt={name} size={240} />
        <div>
          <span className="card__number">{dexNumber(pokemon.id)}</span>
          <h1>{name}</h1>
          <p className="detail__description">{pokemon.description ?? 'No description available.'}</p>
        </div>
      </header>

      <div className="detail__grid">
        <section className="panel">
          <h2>Base stats</h2>
          <StatBars stats={pokemon.stats} />
        </section>

        <section className="panel">
          <h2>Evolution chain</h2>
          <EvolutionTree chain={pokemon.evolutionChain} current={pokemon.name} />
        </section>

        <CollectionPanel id={pokemon.id} name={pokemon.name} />

        <SkillsPanel id={pokemon.id} />
      </div>
    </article>
  );
}

function CollectionPanel({ id, name }: { id: number; name: string }) {
  const { session } = useAuth();
  const { data: stored, isPending, error } = useStoredPokemon(id);

  return (
    <section className="panel">
      <h2>In your collection</h2>
      {isPending ? (
        <Spinner />
      ) : error ? (
        <ErrorState error={error} />
      ) : stored ? (
        <>
          <InCollectionBadge />
          <dl className="facts facts--stacked">
            <div>
              <dt>Category</dt>
              <dd>{stored.category ?? '—'}</dd>
            </div>
            <div>
              <dt>Weight</dt>
              <dd>{weightInKg(stored.weight)}</dd>
            </div>
            <div>
              <dt>Localized name</dt>
              <dd>{stored.localizedName ?? <span className="muted">Not set</span>}</dd>
            </div>
            <div>
              <dt>Region</dt>
              <dd>{stored.region ?? <span className="muted">Not set</span>}</dd>
            </div>
            <div>
              <dt>Tags</dt>
              <dd>
                {stored.tags.length > 0 ? (
                  <span className="tags">
                    {stored.tags.map((tag) => (
                      <span key={tag} className="tag">
                        {tag}
                      </span>
                    ))}
                  </span>
                ) : (
                  <span className="muted">None</span>
                )}
              </dd>
            </div>
          </dl>
          <div className="panel__actions">
            {session && (
              <Link to={`/collection/${id}/edit`} className="button button--small">
                Edit
              </Link>
            )}
            <SyncButton id={id} name={name} stored />
          </div>
        </>
      ) : (
        <>
          <p className="muted">Not saved yet. Save it to add your own name, region and tags.</p>
          <div className="panel__actions">
            <SyncButton id={id} name={name} />
          </div>
        </>
      )}
    </section>
  );
}

function SkillsPanel({ id }: { id: number }) {
  const { data: skills, isPending, error, refetch } = usePokemonSkills(id);

  return (
    <section className="panel panel--wide">
      <h2>
        Skills
        {skills && skills.length > 0 && <span className="panel__count">{skills.length}</span>}
      </h2>
      {isPending ? (
        <Spinner label="Loading skills…" />
      ) : error ? (
        <ErrorState error={error} onRetry={() => refetch()} />
      ) : skills === null ? (
        <p className="muted">Skills are not available for this Pokémon.</p>
      ) : (
        <SkillTable skills={skills} />
      )}
    </section>
  );
}
