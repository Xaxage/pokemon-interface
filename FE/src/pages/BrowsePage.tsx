import { useEffect } from 'react';
import { Pagination } from '../components/Pagination';
import { PokemonCard } from '../components/PokemonCard';
import { EmptyState, ErrorState, SkeletonGrid } from '../components/Status';
import { usePagination } from '../hooks/usePagination';
import { usePokemonPage } from '../hooks/usePokemon';

/** US-01: every Pokémon PokeAPI knows, one page at a time. */
export function BrowsePage() {
  const { page, size, setPage, setSize } = usePagination();
  const { data, error, isPending, isFetching, isPlaceholderData, refetch } = usePokemonPage(page, size);

  // A page past the end (e.g. from an old link) jumps back to the last real page.
  useEffect(() => {
    if (data && data.content.length === 0 && data.totalPages > 0 && page >= data.totalPages) setPage(data.totalPages - 1);
  }, [data, page, setPage]);

  useEffect(() => window.scrollTo({ top: 0, behavior: 'smooth' }), [page]);

  return (
    <section>
      <header className="page-header">
        <div>
          <h1>Browse Pokémon</h1>
          <p className="muted">Live from PokeAPI. Open one for its stats and evolutions, or save it to your collection.</p>
        </div>
      </header>

      {data && (
        <Pagination
          page={page}
          size={size}
          totalPages={data.totalPages}
          count={data.count}
          busy={isFetching}
          onPageChange={setPage}
          onSizeChange={setSize}
        />
      )}

      {isPending ? (
        <SkeletonGrid count={size} />
      ) : error && !data ? (
        <ErrorState error={error} onRetry={() => refetch()} />
      ) : data && data.content.length === 0 ? (
        <EmptyState title="No Pokémon on this page" />
      ) : (
        <ul className={`grid ${isPlaceholderData ? 'grid--stale' : ''}`}>
          {data?.content.map((pokemon) => <PokemonCard key={pokemon.id} pokemon={pokemon} />)}
        </ul>
      )}

      {data && data.content.length > 0 && (
        <Pagination
          page={page}
          size={size}
          totalPages={data.totalPages}
          count={data.count}
          busy={isFetching}
          onPageChange={setPage}
          onSizeChange={setSize}
        />
      )}
    </section>
  );
}
