// Mirrors the backend's web DTOs (com.xaxage.be.adapter.in.web.dto).

export interface LearnMethod {
  method: string;
  level: number | null;
}

export interface Skill {
  name: string;
  methods: LearnMethod[];
}

/** A Pokémon as listed live from PokeAPI. Weight is in hectograms. */
export interface Pokemon {
  id: number;
  name: string;
  sprite: string | null;
  category: string | null;
  weight: number | null;
  skills: Skill[];
}

export interface PokemonPage {
  content: Pokemon[];
  hasNext: boolean;
  page: number;
  size: number;
  count: number;
  totalPages: number;
}

export interface Stat {
  name: string;
  base: number;
}

export interface EvolutionStage {
  name: string;
  evolvesTo: EvolutionStage[];
}

export interface DetailedPokemon {
  id: number;
  name: string;
  image: string | null;
  stats: Stat[];
  description: string | null;
  evolutionChain: EvolutionStage | null;
}

/** Our local copy of a Pokémon: PokeAPI's fields plus our own (localizedName, region, tags). */
export interface StoredPokemon {
  id: number;
  name: string;
  category: string | null;
  weight: number | null;
  sprite: string | null;
  localizedName: string | null;
  region: string | null;
  tags: string[];
}

export interface StoredPokemonPage {
  content: StoredPokemon[];
  page: number;
  size: number;
  count: number;
  totalPages: number;
}

/** A full replace of our own fields; anything left out is cleared. */
export interface EditStoredPokemonRequest {
  localizedName?: string;
  region?: string;
  tags?: string[];
}

export interface Credentials {
  username: string;
  password: string;
}

export interface AccessToken {
  token: string;
}

export interface User {
  username: string;
}

export interface InvalidField {
  field: string;
  reason: string;
}

/** RFC 9457 problem detail: the backend's error body on every endpoint. */
export interface ProblemDetail {
  type?: string;
  title?: string;
  status: number;
  detail?: string;
  instance?: string;
  invalidFields?: InvalidField[];
}
