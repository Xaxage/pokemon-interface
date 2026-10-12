package com.xaxage.be.adapter.out.pokeapi;

import com.xaxage.be.domain.model.Pokemon;
import com.xaxage.be.domain.model.PokemonPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("PokeAPI client")
class PokeApiClientTest {

    @Nested
    @DisplayName("Fetching a page of Pokémon from PokeAPI")
    class FetchingAPage {

        @Test
        @DisplayName("The one where PokeAPI lists Spearow and Fearow and has more after them, so both come back with their ids and a next page")
        void readsEntriesAndNextPageFromPokeApiList() {
            var builder = RestClient.builder().baseUrl("https://pokeapi.test/api/v2");
            var pokeApi = MockRestServiceServer.bindTo(builder).build();
            pokeApi.expect(requestTo("https://pokeapi.test/api/v2/pokemon/?offset=20&limit=2"))
                    .andRespond(withSuccess("""
                            {
                              "count": 1350,
                              "next": "https://pokeapi.test/api/v2/pokemon/?offset=22&limit=2",
                              "previous": "https://pokeapi.test/api/v2/pokemon/?offset=18&limit=2",
                              "results": [
                                {"name": "spearow", "url": "https://pokeapi.test/api/v2/pokemon/21/"},
                                {"name": "fearow", "url": "https://pokeapi.test/api/v2/pokemon/22/"}
                              ]
                            }
                            """, MediaType.APPLICATION_JSON));

            var page = new PokeApiClient(builder.build()).fetchPokemon(20, 2);

            assertThat(page).isEqualTo(new PokemonPage(
                    List.of(new Pokemon(21, "spearow"), new Pokemon(22, "fearow")), true, 1350));
            pokeApi.verify();
        }

        @Test
        @DisplayName("The one where PokeAPI has nothing after the last alternate forms, so they come back with no next page")
        void lastPageHasNoNextPage() {
            var builder = RestClient.builder().baseUrl("https://pokeapi.test/api/v2");
            var pokeApi = MockRestServiceServer.bindTo(builder).build();
            pokeApi.expect(requestTo("https://pokeapi.test/api/v2/pokemon/?offset=1348&limit=20"))
                    .andRespond(withSuccess("""
                            {
                              "count": 1350,
                              "next": null,
                              "previous": "https://pokeapi.test/api/v2/pokemon/?offset=1328&limit=20",
                              "results": [
                                {"name": "pikachu-world-cap", "url": "https://pokeapi.test/api/v2/pokemon/10324/"},
                                {"name": "pikachu-gmax", "url": "https://pokeapi.test/api/v2/pokemon/10325/"}
                              ]
                            }
                            """, MediaType.APPLICATION_JSON));

            var page = new PokeApiClient(builder.build()).fetchPokemon(1348, 20);

            assertThat(page).isEqualTo(new PokemonPage(
                    List.of(new Pokemon(10324, "pikachu-world-cap"), new Pokemon(10325, "pikachu-gmax")), false, 1350));
            pokeApi.verify();
        }

        @Test
        @DisplayName("The one where PokeAPI reports 1350 Pokémon in total, so the page carries that total")
        void carriesTheTotalReportedByPokeApi() {
            var builder = RestClient.builder().baseUrl("https://pokeapi.test/api/v2");
            var pokeApi = MockRestServiceServer.bindTo(builder).build();
            pokeApi.expect(requestTo("https://pokeapi.test/api/v2/pokemon/?offset=0&limit=1"))
                    .andRespond(withSuccess("""
                            {
                              "count": 1350,
                              "next": "https://pokeapi.test/api/v2/pokemon/?offset=1&limit=1",
                              "previous": null,
                              "results": [
                                {"name": "bulbasaur", "url": "https://pokeapi.test/api/v2/pokemon/1/"}
                              ]
                            }
                            """, MediaType.APPLICATION_JSON));

            var page = new PokeApiClient(builder.build()).fetchPokemon(0, 1);

            assertThat(page.count()).isEqualTo(1350);
            pokeApi.verify();
        }
    }

    @Nested
    @DisplayName("Must show each Pokémon's default front sprite")
    class FetchingTheDefaultFrontSprite {

        @Test
        @DisplayName("The one where PokeAPI gives Bulbasaur a default front sprite, so its link comes back with Bulbasaur")
        void readsTheDefaultFrontSprite() {
            var builder = RestClient.builder().baseUrl("https://pokeapi.test/api/v2");
            var pokeApi = MockRestServiceServer.bindTo(builder).build();
            pokeApi.expect(requestTo("https://pokeapi.test/api/v2/pokemon/1/"))
                    .andRespond(withSuccess("""
                            {
                              "id": 1,
                              "name": "bulbasaur",
                              "weight": 69,
                              "sprites": {
                                "front_default": "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png",
                                "front_shiny": "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/shiny/1.png"
                              }
                            }
                            """, MediaType.APPLICATION_JSON));

            var pokemon = new PokeApiClient(builder.build()).fetchPokemonDetails(1);

            assertThat(pokemon).contains(new Pokemon(1, "bulbasaur",
                    "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png"));
            pokeApi.verify();
        }

        @Test
        @DisplayName("The one where PokeAPI gives an alternate form no sprites at all, so it still comes back, with an empty sprite")
        void formWithoutAnySpritesComesBackWithAnEmptySprite() {
            var builder = RestClient.builder().baseUrl("https://pokeapi.test/api/v2");
            var pokeApi = MockRestServiceServer.bindTo(builder).build();
            pokeApi.expect(requestTo("https://pokeapi.test/api/v2/pokemon/10264/"))
                    .andRespond(withSuccess("""
                            {
                              "id": 10264,
                              "name": "koraidon-limited-build",
                              "weight": 3030
                            }
                            """, MediaType.APPLICATION_JSON));

            var pokemon = new PokeApiClient(builder.build()).fetchPokemonDetails(10264);

            assertThat(pokemon).contains(new Pokemon(10264, "koraidon-limited-build", null));
            pokeApi.verify();
        }

        @Test
        @DisplayName("The one where PokeAPI has no details for a listed Pokémon, so no details come back instead of an error")
        void missingDetailsComeBackEmpty() {
            var builder = RestClient.builder().baseUrl("https://pokeapi.test/api/v2");
            var pokeApi = MockRestServiceServer.bindTo(builder).build();
            pokeApi.expect(requestTo("https://pokeapi.test/api/v2/pokemon/2/"))
                    .andRespond(withStatus(HttpStatus.NOT_FOUND));

            var pokemon = new PokeApiClient(builder.build()).fetchPokemonDetails(2);

            assertThat(pokemon).isEmpty();
            pokeApi.verify();
        }
    }
}