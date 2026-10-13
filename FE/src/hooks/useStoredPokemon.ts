import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ApiError } from '../api/client';
import { storedPokemonApi } from '../api/storedPokemon';
import type { EditStoredPokemonRequest, StoredPokemon } from '../api/types';

export const storedKeys = {
  all: ['stored-pokemon'] as const,
  pages: () => [...storedKeys.all, 'page'] as const,
  page: (page: number, size: number) => [...storedKeys.pages(), page, size] as const,
  detail: (id: number) => [...storedKeys.all, 'detail', id] as const,
};

export function useStoredPokemonPage(page: number, size: number) {
  return useQuery({
    queryKey: storedKeys.page(page, size),
    queryFn: ({ signal }) => storedPokemonApi.browse(page, size, signal),
    placeholderData: keepPreviousData,
  });
}

/** The stored copy of a Pokémon, or null when it is not in the collection. */
export function useStoredPokemon(id: number) {
  return useQuery({
    queryKey: storedKeys.detail(id),
    queryFn: async ({ signal }): Promise<StoredPokemon | null> => {
      try {
        return await storedPokemonApi.view(id, signal);
      } catch (error) {
        if (error instanceof ApiError && error.status === 404) return null;
        throw error;
      }
    },
  });
}

/** Keeps every cached view of the collection in step with a change the server confirmed. */
function useCollectionCache() {
  const queryClient = useQueryClient();
  return {
    stored: (pokemon: StoredPokemon) => {
      queryClient.setQueryData(storedKeys.detail(pokemon.id), pokemon);
      return queryClient.invalidateQueries({ queryKey: storedKeys.pages() });
    },
    removed: (id: number) => {
      queryClient.setQueryData(storedKeys.detail(id), null);
      return queryClient.invalidateQueries({ queryKey: storedKeys.pages() });
    },
  };
}

/** Adds a Pokémon to the collection, or refreshes PokeAPI's fields of one already there. */
export function useSynchronisePokemon() {
  const cache = useCollectionCache();
  return useMutation({
    mutationFn: (id: number) => storedPokemonApi.synchronise(id),
    onSuccess: cache.stored,
  });
}

export function useEditStoredPokemon(id: number) {
  const cache = useCollectionCache();
  return useMutation({
    mutationFn: (fields: EditStoredPokemonRequest) => storedPokemonApi.edit(id, fields),
    onSuccess: cache.stored,
  });
}

export function useDeleteStoredPokemon() {
  const cache = useCollectionCache();
  return useMutation({
    mutationFn: (id: number) => storedPokemonApi.remove(id),
    onSuccess: (_, id) => cache.removed(id),
  });
}
