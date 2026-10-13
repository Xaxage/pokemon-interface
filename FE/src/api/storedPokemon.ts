import { request } from './client';
import type { EditStoredPokemonRequest, StoredPokemon, StoredPokemonPage } from './types';

export const storedPokemonApi = {
  browse: (page: number, size: number, signal?: AbortSignal) =>
    request<StoredPokemonPage>('/stored-pokemon', { query: { page, size }, signal }),

  view: (id: number, signal?: AbortSignal) => request<StoredPokemon>(`/stored-pokemon/${id}`, { signal }),

  /** Copies the Pokémon from PokeAPI (created), or refreshes PokeAPI's fields of an existing copy. */
  synchronise: (id: number) => request<StoredPokemon>(`/stored-pokemon/${id}/sync`, { method: 'POST' }),

  edit: (id: number, fields: EditStoredPokemonRequest) =>
    request<StoredPokemon>(`/stored-pokemon/${id}`, { method: 'PUT', body: fields }),

  remove: (id: number) => request<void>(`/stored-pokemon/${id}`, { method: 'DELETE' }),
};
