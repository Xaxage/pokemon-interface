import { keepPreviousData, useQuery, useQueryClient, type QueryClient } from '@tanstack/react-query';
import { useCallback, useEffect } from 'react';
import { pokemonApi } from '../api/pokemon';
import type { PokemonPage, Skill } from '../api/types';

export const pokemonKeys = {
  all: ['pokemon'] as const,
  pages: () => [...pokemonKeys.all, 'page'] as const,
  page: (page: number, size: number) => [...pokemonKeys.pages(), page, size] as const,
  detail: (id: number) => [...pokemonKeys.all, 'detail', id] as const,
  skills: (id: number) => [...pokemonKeys.all, 'skills', id] as const,
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

/**
 * A Pokémon's skills, or null when they can't be found. The detail endpoint has no skills, but the list endpoint
 * does, so they come from a browse page already in the cache (the usual case, arriving from a card) or else from a
 * one-Pokémon page at the Pokémon's place in the list.
 */
export function usePokemonSkills(id: number) {
  const queryClient = useQueryClient();
  return useQuery({
    queryKey: pokemonKeys.skills(id),
    queryFn: ({ signal }) => findSkills(queryClient, id, signal),
    staleTime: PAGE_STALE_TIME,
  });
}

async function findSkills(queryClient: QueryClient, id: number, signal: AbortSignal): Promise<Skill[] | null> {
  const cached = queryClient
    .getQueriesData<PokemonPage>({ queryKey: pokemonKeys.pages() })
    .flatMap(([, page]) => page?.content ?? [])
    .find((pokemon) => pokemon.id === id);
  if (cached) return cached.skills;

  // PokeAPI lists the base Pokémon first, in id order (#1 at index 0), then the alternate forms (ids from 10001),
  // also in id order. Every guess is checked against the id that comes back.
  const atIndex = async (index: number) => (index >= 0 ? (await pokemonApi.browse(index, 1, signal)).content[0] : undefined);

  const guess = await pokemonApi.browse(id - 1, 1, signal);
  if (guess.content[0]?.id === id) return guess.content[0].skills;

  const last = await atIndex(guess.count - 1);
  if (!last) return null;
  const form = await atIndex(guess.count - 1 - (last.id - id));
  return form?.id === id ? form.skills : null;
}
