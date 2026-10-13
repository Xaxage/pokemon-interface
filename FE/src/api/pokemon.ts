import { request } from './client';
import type { DetailedPokemon, PokemonPage } from './types';

export const pokemonApi = {
  browse: (page: number, size: number, signal?: AbortSignal) =>
    request<PokemonPage>('/pokemon', { query: { page, size }, signal }),

  view: (id: number, signal?: AbortSignal) => request<DetailedPokemon>(`/pokemon/${id}`, { signal }),
};
