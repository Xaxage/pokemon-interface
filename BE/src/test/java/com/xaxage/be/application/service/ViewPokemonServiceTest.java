package com.xaxage.be.application.service;

import com.xaxage.be.application.port.out.PokeApiPort;
import com.xaxage.be.domain.exception.PokemonNotFoundException;
import com.xaxage.be.domain.model.DetailedPokemon;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@DisplayName("Viewing one Pokémon")
class ViewPokemonServiceTest {

    @Nested
    @DisplayName("Must show a chosen Pokémon by its id")
    class ShowsAChosenPokemonByItsId {

        @Test
        @DisplayName("The one where id 1 is asked for, so PokeAPI's Bulbasaur is returned")
        void returnsThePokemonPokeApiHasForTheId() {
            var pokeApi = mock(PokeApiPort.class);
            given(pokeApi.fetchDetailedPokemon(1)).willReturn(Optional.of(new DetailedPokemon(1, "bulbasaur")));

            var pokemon = new ViewPokemonService(pokeApi).view(1);

            assertThat(pokemon).isEqualTo(new DetailedPokemon(1, "bulbasaur"));
        }

        @Test
        @DisplayName("The one where PokeAPI has no Pokémon with id 99999, so it is reported as not found")
        void unknownIdIsReportedAsNotFound() {
            var pokeApi = mock(PokeApiPort.class);
            given(pokeApi.fetchDetailedPokemon(99999)).willReturn(Optional.empty());

            var service = new ViewPokemonService(pokeApi);

            assertThatThrownBy(() -> service.view(99999))
                    .isInstanceOf(PokemonNotFoundException.class)
                    .hasMessage("No Pokémon has the id 99999");
        }
    }
}
