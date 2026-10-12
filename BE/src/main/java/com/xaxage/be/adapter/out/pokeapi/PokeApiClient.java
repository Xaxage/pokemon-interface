package com.xaxage.be.adapter.out.pokeapi;

import com.xaxage.be.adapter.out.pokeapi.dto.PokemonDetailResponse;
import com.xaxage.be.adapter.out.pokeapi.dto.PokemonListResponse;
import com.xaxage.be.application.port.out.PokeApiPort;
import com.xaxage.be.domain.model.Pokemon;
import com.xaxage.be.domain.model.PokemonPage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Component
class PokeApiClient implements PokeApiPort {

    private final RestClient pokeApi;

    PokeApiClient(@Qualifier(PokeApiConfig.POKE_API_REST_CLIENT) RestClient pokeApi) {
        this.pokeApi = pokeApi;
    }

    @Override
    public PokemonPage fetchPokemon(int offset, int limit) {
        return pokeApi.get()
                .uri("/pokemon/?offset={offset}&limit={limit}", offset, limit)
                .retrieve()
                .body(PokemonListResponse.class)
                .toPokemonPage();
    }

    @Override
    public Optional<Pokemon> fetchPokemonDetails(int id) {
        try {
            return Optional.of(pokeApi.get()
                    .uri("/pokemon/{id}/", id)
                    .retrieve()
                    .body(PokemonDetailResponse.class)
                    .toPokemon());
        } catch (HttpClientErrorException.NotFound noDetails) {
            return Optional.empty();
        }
    }
}
