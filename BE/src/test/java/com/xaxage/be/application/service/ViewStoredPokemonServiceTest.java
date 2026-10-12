package com.xaxage.be.application.service;

import com.xaxage.be.application.port.out.StoredPokemonRepositoryPort;
import com.xaxage.be.domain.exception.PokemonNotStoredException;
import com.xaxage.be.domain.model.StoredPokemon;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@DisplayName("Viewing one Pokémon stored in the local database")
class ViewStoredPokemonServiceTest {

    @Nested
    @DisplayName("Must show one stored Pokémon by its id")
    class ShowsOneStoredPokemonByItsId {

        @Test
        @DisplayName("The one where Bulbasaur is stored, so it is returned with PokeAPI's fields and our own")
        void returnsTheStoredPokemon() {
            var storedPokemon = mock(StoredPokemonRepositoryPort.class);
            var bulbasaur = new StoredPokemon(1, "bulbasaur", "Seed Pokémon", 69, "bulbasaur.png",
                    "Bisasam", "Kanto", List.of("starter"));
            given(storedPokemon.findById(1)).willReturn(Optional.of(bulbasaur));

            var viewed = new ViewStoredPokemonService(storedPokemon).view(1);

            assertThat(viewed).isEqualTo(bulbasaur);
        }

        @Test
        @DisplayName("The one where id 1 is not stored locally, so it is reported as not stored")
        void pokemonNotStoredLocallyIsReportedAsNotStored() {
            var storedPokemon = mock(StoredPokemonRepositoryPort.class);
            given(storedPokemon.findById(1)).willReturn(Optional.empty());

            var service = new ViewStoredPokemonService(storedPokemon);

            assertThatThrownBy(() -> service.view(1))
                    .isInstanceOf(PokemonNotStoredException.class)
                    .hasMessage("No Pokémon with the id 1 is stored");
        }
    }
}
