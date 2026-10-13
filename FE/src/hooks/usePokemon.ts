import { keepPreviousData, useQuery, useQueryClient } from '@tanstack/react-query';
import { useCallback, useEffect } from 'react';
import { pokemonApi } from '../api/pokemon';

export const pokemonKeys = {
  all: ['pokemon'] as const,
  page: (page: number, size: number) => [...pokemonKeys.all, 'page', page, size] as const,
  detail: (id: number) => [...pokemonKeys.all, 'detail', id] as const,
};

const PAGE_STALE_TIME = 5 * 60_000;
const DETAIL_STALE_TIME = 10 * 60_000;

function pageQuery(page: number, size: number) {
  return {
    queryKey: pokemonKeys.page(page, size),
    queryFn: ({ signal }: { signal: AbortSignal }) => pokemonApi.browse(page, size, signal),
    staleTime: PAGE_STALE_TIME,
  };
}

function detailQuery(id: number) {
  return {
    queryKey: pokemonKeys.detail(id),
    queryFn: ({ signal }: { signal: AbortSignal }) => pokemonApi.view(id, signal),
    staleTime: DETAIL_STALE_TIME,
  };
}

/** One page of Pokémon; once it is shown, the next page is fetched in the background so paging feels instant. */
export function usePokemonPage(page: number, size: number) {
  const queryClient = useQueryClient();
  const query = useQuery({ ...pageQuery(page, size), placeholderData: keepPreviousData });
  const hasNext = query.data?.hasNext && !query.isPlaceholderData;

  useEffect(() => {
    if (hasNext) void queryClient.prefetchQuery(pageQuery(page + 1, size));
  }, [hasNext, page, size, queryClient]);

  return query;
}

export function usePokemonDetails(id: number) {
  return useQuery(detailQuery(id));
}

/** Starts loading a Pokémon's detail page early, e.g. when the reader points at its card. */
export function usePrefetchPokemonDetails() {
  const queryClient = useQueryClient();
  return useCallback((id: number) => void queryClient.prefetchQuery(detailQuery(id)), [queryClient]);
}
