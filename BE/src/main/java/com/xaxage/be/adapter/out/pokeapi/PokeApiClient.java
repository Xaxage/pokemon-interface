package com.xaxage.be.adapter.out.pokeapi;

import com.xaxage.be.adapter.out.pokeapi.dto.*;
import com.xaxage.be.application.port.out.PokeApiPort;
import com.xaxage.be.application.port.out.PokeApiUnavailableException;
import com.xaxage.be.domain.model.DetailedPokemon;
import com.xaxage.be.domain.model.EvolutionStage;
import com.xaxage.be.domain.model.Pokemon;
import com.xaxage.be.domain.model.PokemonPage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;
import java.util.function.Supplier;

@Component
class PokeApiClient implements PokeApiPort {

    private final RestClient pokeApi;

    PokeApiClient(@Qualifier(PokeApiConfig.POKE_API_REST_CLIENT) RestClient pokeApi) {
        this.pokeApi = pokeApi;
    }

    @Override
    @Cacheable(PokeApiCacheConfig.POKEMON_PAGES)
    public PokemonPage fetchPokemon(int offset, int limit) {
        return unlessUnavailable(() -> pokeApi.get()
                .uri("/pokemon/?offset={offset}&limit={limit}", offset, limit)
                .retrieve()
                .body(PokemonListResponse.class)
                .toPokemonPage());
    }

    @Override
    @Cacheable(PokeApiCacheConfig.POKEMON_DETAILS)
    public Optional<Pokemon> fetchPokemonDetails(int id) {
        return unlessUnavailable(() -> pokemon(id)
                .map(details -> details.toPokemon().withCategory(categoryOf(details.species()))));
    }

    /**
     * PokeAPI lists version groups oldest first, so the newest is the last one listed.
     */
    @Override
    @Cacheable(PokeApiCacheConfig.NEWEST_VERSION_GROUP)
    public String fetchNewestVersionGroup() {
        return unlessUnavailable(() -> {
            var total = versionGroups(0, 1).count();
            return versionGroups(total - 1, 1).results().getFirst().name();
        });
    }

    @Override
    @Cacheable(PokeApiCacheConfig.DETAILED_POKEMON)
    public Optional<DetailedPokemon> fetchDetailedPokemon(int id) {
        return unlessUnavailable(() -> pokemon(id)
                .map(details -> withSpeciesDetails(details.toDetailedPokemon(), details.species())));
    }

    /**
     * The description and the evolution chain belong to the Pokémon's species, not to the Pokémon itself.
     */
    private DetailedPokemon withSpeciesDetails(DetailedPokemon pokemon, NamedResource species) {
        if (species == null) {
            return pokemon;
        }
        var speciesDetails = species(species);
        return pokemon
                .withDescription(speciesDetails.newestEnglishFlavorText())
                .withEvolutionChain(firstStageOf(speciesDetails.evolutionChain()));
    }

    private EvolutionStage firstStageOf(NamedResource evolutionChain) {
        if (evolutionChain == null) {
            return null;
        }
        return pokeApi.get()
                .uri("/evolution-chain/{id}/", evolutionChain.id())
                .retrieve()
                .body(EvolutionChainResponse.class)
                .toFirstStage();
    }

    /**
     * @return PokeAPI's {@code /pokemon/{id}} resource, or empty when PokeAPI has no Pokémon with the id
     */
    private Optional<PokemonDetailResponse> pokemon(int id) {
        try {
            return Optional.of(pokeApi.get()
                    .uri("/pokemon/{id}/", id)
                    .retrieve()
                    .body(PokemonDetailResponse.class));
        } catch (HttpClientErrorException.NotFound noSuchPokemon) {
            return Optional.empty();
        }
    }

    private VersionGroupListResponse versionGroups(int offset, int limit) {
        return pokeApi.get()
                .uri("/version-group/?offset={offset}&limit={limit}", offset, limit)
                .retrieve()
                .body(VersionGroupListResponse.class);
    }

    private String categoryOf(NamedResource species) {
        if (species == null) {
            return null;
        }
        return species(species).englishGenus();
    }

    private PokemonSpeciesResponse species(NamedResource species) {
        return pokeApi.get()
                .uri("/pokemon-species/{id}/", species.id())
                .retrieve()
                .body(PokemonSpeciesResponse.class);
    }

    /**
     * Any failure to reach PokeAPI, or to get an answer from it, means its data is unavailable right now.
     */
    private static <T> T unlessUnavailable(Supplier<T> call) {
        try {
            return call.get();
        } catch (RestClientException unavailable) {
            throw new PokeApiUnavailableException(unavailable);
        }
    }
}
