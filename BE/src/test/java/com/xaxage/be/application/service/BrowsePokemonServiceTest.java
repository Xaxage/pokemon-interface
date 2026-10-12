package com.xaxage.be.application.service;

import com.xaxage.be.application.port.out.PokeApiPort;
import com.xaxage.be.domain.model.Pokemon;
import com.xaxage.be.domain.model.PokemonPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

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
            given(pokeApi.fetchPokemonDetails(21)).willReturn(Optional.of(new Pokemon(21, "spearow")));
            given(pokeApi.fetchPokemonDetails(40)).willReturn(Optional.of(new Pokemon(40, "wigglytuff")));

            var page = new BrowsePokemonService(pokeApi).browse(1, 20);

            assertThat(page).isEqualTo(secondPage);
        }

        @Test
        @DisplayName("The one where page 0 of size 20 asks PokeAPI for the first 20 entries, starting from the very beginning")
        void firstPageStartsAtTheBeginningOfPokeApi() {
            var pokeApi = mock(PokeApiPort.class);
            var firstPage = new PokemonPage(List.of(new Pokemon(1, "bulbasaur"), new Pokemon(20, "raticate")), true, 1350);
            given(pokeApi.fetchPokemon(0, 20)).willReturn(firstPage);
            given(pokeApi.fetchPokemonDetails(1)).willReturn(Optional.of(new Pokemon(1, "bulbasaur")));
            given(pokeApi.fetchPokemonDetails(20)).willReturn(Optional.of(new Pokemon(20, "raticate")));

            var page = new BrowsePokemonService(pokeApi).browse(0, 20);

            assertThat(page).isEqualTo(firstPage);
        }
    }

    @Nested
    @DisplayName("Must show each Pokémon's default front sprite")
    class ShowsEachPokemonsDefaultFrontSprite {

        @Test
        @DisplayName("The one where Bulbasaur is listed, so its details are fetched and it comes back with its default front sprite")
        void listedPokemonComeBackWithTheirSprite() {
            var pokeApi = mock(PokeApiPort.class);
            given(pokeApi.fetchPokemon(0, 20))
                    .willReturn(new PokemonPage(List.of(new Pokemon(1, "bulbasaur")), false, 1));
            given(pokeApi.fetchPokemonDetails(1))
                    .willReturn(Optional.of(new Pokemon(1, "bulbasaur",
                            "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png")));

            var page = new BrowsePokemonService(pokeApi).browse(0, 20);

            assertThat(page).isEqualTo(new PokemonPage(List.of(new Pokemon(1, "bulbasaur",
                    "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png")), false, 1));
        }

        @Test
        @DisplayName("The one where PokeAPI has no details for listed Ivysaur, so it is still listed, with only its name and an empty sprite")
        void listedPokemonWithoutDetailsIsKeptAsListed() {
            var pokeApi = mock(PokeApiPort.class);
            given(pokeApi.fetchPokemon(0, 20))
                    .willReturn(new PokemonPage(List.of(new Pokemon(2, "ivysaur")), false, 1));
            given(pokeApi.fetchPokemonDetails(2)).willReturn(Optional.empty());

            var page = new BrowsePokemonService(pokeApi).browse(0, 20);

            assertThat(page).isEqualTo(new PokemonPage(List.of(new Pokemon(2, "ivysaur", null)), false, 1));
        }
    }
}