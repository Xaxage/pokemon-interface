package com.xaxage.be.application.service;

import com.xaxage.be.application.port.out.PokeApiPort;
import com.xaxage.be.domain.model.Pokemon;
import com.xaxage.be.domain.model.PokemonPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@DisplayName("Browsing Pokémon")
class BrowsePokemonServiceTest {

    @Nested
    @DisplayName("Must return Pokémon one page at a time, in PokeAPI order")
    class OnePageAtATime {

        @Test
        @DisplayName("The one where page 1 of size 20 asks PokeAPI for the 20 entries after the first 20 and returns them")
        void asksPokeApiForTheEntriesOfTheRequestedPage() {
            var pokeApi = mock(PokeApiPort.class);
            var secondPage = new PokemonPage(List.of(new Pokemon(21, "spearow"), new Pokemon(40, "wigglytuff")), true, 1350);
            given(pokeApi.fetchPokemon(20, 20)).willReturn(secondPage);

            var page = new BrowsePokemonService(pokeApi).browse(1, 20);

            assertThat(page).isEqualTo(secondPage);
        }

        @Test
        @DisplayName("The one where page 0 of size 20 asks PokeAPI for the first 20 entries, starting from the very beginning")
        void firstPageStartsAtTheBeginningOfPokeApi() {
            var pokeApi = mock(PokeApiPort.class);
            var firstPage = new PokemonPage(List.of(new Pokemon(1, "bulbasaur"), new Pokemon(20, "raticate")), true, 1350);
            given(pokeApi.fetchPokemon(0, 20)).willReturn(firstPage);

            var page = new BrowsePokemonService(pokeApi).browse(0, 20);

            assertThat(page).isEqualTo(firstPage);
        }
    }
}