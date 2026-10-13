import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { pokemonApi } from '../api/pokemon';

export const pokemonKeys = {
  all: ['pokemon'] as const,
  page: (page: number, size: number) => [...pokemonKeys.all, 'page', page, size] as const,
  detail: (id: number) => [...pokemonKeys.all, 'detail', id] as const,
};

export function usePokemonPage(page: number, size: number) {
  return useQuery({
    queryKey: pokemonKeys.page(page, size),
    queryFn: ({ signal }) => pokemonApi.browse(page, size, signal),
    placeholderData: keepPreviousData,
    staleTime: 5 * 60_000,
  });
}

export function usePokemonDetails(id: number) {
  return useQuery({
    queryKey: pokemonKeys.detail(id),
    queryFn: ({ signal }) => pokemonApi.view(id, signal),
    staleTime: 10 * 60_000,
  });
}
