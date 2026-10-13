package com.xaxage.be.application.service;

import com.xaxage.be.application.port.in.BrowsePokemonUseCase;
import com.xaxage.be.application.port.out.PokeApiPort;
import com.xaxage.be.domain.model.Pokemon;
import com.xaxage.be.domain.model.PokemonPage;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;

@Service
public class BrowsePokemonService implements BrowsePokemonUseCase {

    private final PokeApiPort pokeApi;

    public BrowsePokemonService(PokeApiPort pokeApi) {
        this.pokeApi = pokeApi;
    }

    @Override
    public PokemonPage browse(int page, int size) {
        var listed = pokeApi.fetchPokemon(page * size, size);
        var newestVersionGroup = pokeApi.fetchNewestVersionGroup();
        var detailed = withDetails(listed.content()).stream()
                .map(pokemon -> pokemon.learnableIn(newestVersionGroup))
                .toList();
        return new PokemonPage(detailed, listed.hasNext(), listed.count());
    }

    /**
     * Each Pokémon's details are a separate PokeAPI call, so they are all fetched at once, on virtual threads,
     * and joined back in listing order.
     */
    private List<Pokemon> withDetails(List<Pokemon> listed) {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var fetches = listed.stream()
                    .map(pokemon -> CompletableFuture.supplyAsync(
                            () -> pokeApi.fetchPokemonDetails(pokemon.id()).orElse(pokemon), executor))
                    .toList();
            return fetches.stream().map(BrowsePokemonService::join).toList();
        }
    }

    /**
     * Rethrows a failed fetch as the exception it failed with (e.g. PokeAPI being unavailable), not wrapped.
     */
    private static Pokemon join(CompletableFuture<Pokemon> fetch) {
        try {
            return fetch.join();
        } catch (CompletionException failed) {
            if (failed.getCause() instanceof RuntimeException cause) {
                throw cause;
            }
            throw failed;
        }
    }
}
