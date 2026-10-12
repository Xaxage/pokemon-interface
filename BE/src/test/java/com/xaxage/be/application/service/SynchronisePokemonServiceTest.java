package com.xaxage.be.application.service;

import com.xaxage.be.application.port.out.PokeApiPort;
import com.xaxage.be.application.port.out.StoredPokemonRepositoryPort;
import com.xaxage.be.domain.exception.PokemonNotFoundException;
import com.xaxage.be.domain.model.Pokemon;
import com.xaxage.be.domain.model.StoredPokemon;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@DisplayName("Synchronising a Pokémon from PokeAPI")
class SynchronisePokemonServiceTest {

    @Nested
    @DisplayName("Must copy a Pokémon from PokeAPI into the local database, with our own fields empty")
    class CopiesAPokemonFromPokeApi {

        @Test
        @DisplayName("The one where Bulbasaur is not stored yet, so PokeAPI's Bulbasaur is stored with our own fields empty")
        void storesPokeApisPokemonWithOurOwnFieldsEmpty() {
            var pokeApi = mock(PokeApiPort.class);
            var storedPokemon = mock(StoredPokemonRepositoryPort.class);
            given(pokeApi.fetchPokemonDetails(1)).willReturn(Optional.of(new Pokemon(1, "bulbasaur")
                    .withCategory("Seed Pokémon").withWeight(69).withSprite("bulbasaur.png")));
            given(storedPokemon.save(any())).willAnswer(invocation -> invocation.getArgument(0));

            var synchronised = new SynchronisePokemonService(pokeApi, storedPokemon).synchronise(1);

            var expected = new StoredPokemon(1, "bulbasaur", "Seed Pokémon", 69, "bulbasaur.png", null, null, List.of());
            then(storedPokemon).should().save(expected);
            assertThat(synchronised).isEqualTo(expected);
        }

        @Test
        @DisplayName("The one where PokeAPI has no Pokémon with id 99999, so it is reported as not found and nothing is stored")
        void unknownPokemonIsReportedAsNotFoundAndNotStored() {
            var pokeApi = mock(PokeApiPort.class);
            var storedPokemon = mock(StoredPokemonRepositoryPort.class);
            given(pokeApi.fetchPokemonDetails(99999)).willReturn(Optional.empty());

            var service = new SynchronisePokemonService(pokeApi, storedPokemon);

            assertThatThrownBy(() -> service.synchronise(99999))
                    .isInstanceOf(PokemonNotFoundException.class)
                    .hasMessage("No Pokémon has the id 99999");
            then(storedPokemon).shouldHaveNoInteractions();
        }
    }
}
