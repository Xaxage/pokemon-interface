package com.xaxage.be.adapter.out.pokeapi;

import com.xaxage.be.adapter.out.pokeapi.dto.*;
import com.xaxage.be.application.port.out.PokeApiPort;
import com.xaxage.be.application.port.out.PokeApiUnavailableException;
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
        return unlessUnavailable(() -> {
            try {
                var details = pokeApi.get()
                        .uri("/pokemon/{id}/", id)
                        .retrieve()
                        .body(PokemonDetailResponse.class);
                return Optional.of(details.toPokemon().withCategory(categoryOf(details.species())));
            } catch (HttpClientErrorException.NotFound noDetails) {
                return Optional.empty();
            }
        });
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
        return pokeApi.get()
                .uri("/pokemon-species/{id}/", species.id())
                .retrieve()
                .body(PokemonSpeciesResponse.class)
                .englishGenus();
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
