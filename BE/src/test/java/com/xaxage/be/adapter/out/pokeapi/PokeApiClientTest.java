package com.xaxage.be.adapter.out.pokeapi;

import com.xaxage.be.application.port.out.PokeApiUnavailableException;
import com.xaxage.be.domain.model.LearnMethod;
import com.xaxage.be.domain.model.Pokemon;
import com.xaxage.be.domain.model.PokemonPage;
import com.xaxage.be.domain.model.Skill;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

            assertThat(pokemon).contains(new Pokemon(1, "bulbasaur")
                    .withSprite("https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png")
                    .withWeight(69));
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

            assertThat(pokemon).contains(new Pokemon(10264, "koraidon-limited-build").withWeight(3030));
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

    @Nested
    @DisplayName("Must show each Pokémon's category as its English species genus")
    class FetchingTheCategory {

        @Test
        @DisplayName("The one where Bulbasaur's species has a Japanese and an English genus, so its category is the English \"Seed Pokémon\"")
        void readsTheEnglishGenusOfTheSpecies() {
            var builder = RestClient.builder().baseUrl("https://pokeapi.test/api/v2");
            var pokeApi = MockRestServiceServer.bindTo(builder).build();
            pokeApi.expect(requestTo("https://pokeapi.test/api/v2/pokemon/1/"))
                    .andRespond(withSuccess("""
                            {
                              "id": 1,
                              "name": "bulbasaur",
                              "sprites": {"front_default": null},
                              "species": {"name": "bulbasaur", "url": "https://pokeapi.test/api/v2/pokemon-species/1/"}
                            }
                            """, MediaType.APPLICATION_JSON));
            pokeApi.expect(requestTo("https://pokeapi.test/api/v2/pokemon-species/1/"))
                    .andRespond(withSuccess("""
                            {
                              "id": 1,
                              "genera": [
                                {"genus": "たねポケモン", "language": {"name": "ja", "url": "https://pokeapi.test/api/v2/language/11/"}},
                                {"genus": "Seed Pokémon", "language": {"name": "en", "url": "https://pokeapi.test/api/v2/language/9/"}}
                              ]
                            }
                            """, MediaType.APPLICATION_JSON));

            var pokemon = new PokeApiClient(builder.build()).fetchPokemonDetails(1);

            assertThat(pokemon).contains(new Pokemon(1, "bulbasaur").withCategory("Seed Pokémon"));
            pokeApi.verify();
        }
    }

    @Nested
    @DisplayName("Must show each Pokémon's weight in hectograms, exactly as PokeAPI gives it")
    class FetchingTheWeight {

        @Test
        @DisplayName("The one where PokeAPI gives Celesteela, the heaviest, a weight of 9999, so 9999 hg comes back")
        void readsTheWeightExactlyAsGiven() {
            var builder = RestClient.builder().baseUrl("https://pokeapi.test/api/v2");
            var pokeApi = MockRestServiceServer.bindTo(builder).build();
            pokeApi.expect(requestTo("https://pokeapi.test/api/v2/pokemon/797/"))
                    .andRespond(withSuccess("""
                            {"id": 797, "name": "celesteela", "weight": 9999, "sprites": {"front_default": null}}
                            """, MediaType.APPLICATION_JSON));

            var pokemon = new PokeApiClient(builder.build()).fetchPokemonDetails(797);

            assertThat(pokemon).contains(new Pokemon(797, "celesteela").withWeight(9999));
            pokeApi.verify();
        }
    }

    @Nested
    @DisplayName("Must show each Pokémon's skills (its moves) and how each is learned, in the newest version group known to PokeAPI")
    class FetchingTheSkills {

        @Test
        @DisplayName("The one where Bulbasaur learns Tackle by level-up at level 1 in Scarlet/Violet, so that skill comes back")
        void readsMovesLearnedByLevelUp() {
            var builder = RestClient.builder().baseUrl("https://pokeapi.test/api/v2");
            var pokeApi = MockRestServiceServer.bindTo(builder).build();
            pokeApi.expect(requestTo("https://pokeapi.test/api/v2/pokemon/1/"))
                    .andRespond(withSuccess("""
                            {
                              "id": 1,
                              "name": "bulbasaur",
                              "moves": [
                                {
                                  "move": {"name": "tackle", "url": "https://pokeapi.test/api/v2/move/33/"},
                                  "version_group_details": [
                                    {
                                      "level_learned_at": 1,
                                      "move_learn_method": {"name": "level-up", "url": "https://pokeapi.test/api/v2/move-learn-method/1/"},
                                      "version_group": {"name": "scarlet-violet", "url": "https://pokeapi.test/api/v2/version-group/25/"}
                                    }
                                  ]
                                }
                              ]
                            }
                            """, MediaType.APPLICATION_JSON));

            var pokemon = new PokeApiClient(builder.build()).fetchPokemonDetails(1);

            assertThat(pokemon).map(Pokemon::skills).contains(List.of(
                    new Skill("tackle", List.of(new LearnMethod("level-up", 1, "scarlet-violet")))));
            pokeApi.verify();
        }

        @Test
        @DisplayName("The one where PokeAPI reports level 0 for Swords Dance learned by machine, so it comes back with no level")
        void movesNotLearnedByLevelUpHaveNoLevel() {
            var builder = RestClient.builder().baseUrl("https://pokeapi.test/api/v2");
            var pokeApi = MockRestServiceServer.bindTo(builder).build();
            pokeApi.expect(requestTo("https://pokeapi.test/api/v2/pokemon/1/"))
                    .andRespond(withSuccess("""
                            {
                              "id": 1,
                              "name": "bulbasaur",
                              "moves": [
                                {
                                  "move": {"name": "swords-dance", "url": "https://pokeapi.test/api/v2/move/14/"},
                                  "version_group_details": [
                                    {
                                      "level_learned_at": 0,
                                      "move_learn_method": {"name": "machine", "url": "https://pokeapi.test/api/v2/move-learn-method/4/"},
                                      "version_group": {"name": "scarlet-violet", "url": "https://pokeapi.test/api/v2/version-group/25/"}
                                    }
                                  ]
                                }
                              ]
                            }
                            """, MediaType.APPLICATION_JSON));

            var pokemon = new PokeApiClient(builder.build()).fetchPokemonDetails(1);

            assertThat(pokemon).map(Pokemon::skills).contains(List.of(
                    new Skill("swords-dance", List.of(new LearnMethod("machine", null, "scarlet-violet")))));
            pokeApi.verify();
        }

        @Test
        @DisplayName("The one where PokeAPI knows 27 version groups, so the newest is the last one listed, The Indigo Disk")
        void newestVersionGroupIsTheLastOneListed() {
            var builder = RestClient.builder().baseUrl("https://pokeapi.test/api/v2");
            var pokeApi = MockRestServiceServer.bindTo(builder).build();
            pokeApi.expect(requestTo("https://pokeapi.test/api/v2/version-group/?offset=0&limit=1"))
                    .andRespond(withSuccess("""
                            {
                              "count": 27,
                              "next": "https://pokeapi.test/api/v2/version-group/?offset=1&limit=1",
                              "previous": null,
                              "results": [{"name": "red-blue", "url": "https://pokeapi.test/api/v2/version-group/1/"}]
                            }
                            """, MediaType.APPLICATION_JSON));
            pokeApi.expect(requestTo("https://pokeapi.test/api/v2/version-group/?offset=26&limit=1"))
                    .andRespond(withSuccess("""
                            {
                              "count": 27,
                              "next": null,
                              "previous": "https://pokeapi.test/api/v2/version-group/?offset=25&limit=1",
                              "results": [{"name": "the-indigo-disk", "url": "https://pokeapi.test/api/v2/version-group/27/"}]
                            }
                            """, MediaType.APPLICATION_JSON));

            var newest = new PokeApiClient(builder.build()).fetchNewestVersionGroup();

            assertThat(newest).isEqualTo("the-indigo-disk");
            pokeApi.verify();
        }
    }

    @Nested
    @DisplayName("Must keep serving cached data while PokeAPI is unavailable, and otherwise fail clearly")
    class WhenPokeApiIsUnavailable {

        @Test
        @DisplayName("The one where PokeAPI answers 503 for a page of Pokémon, so it is reported as unavailable")
        void pageFromPokeApiThatIsDownIsReportedAsUnavailable() {
            var builder = RestClient.builder().baseUrl("https://pokeapi.test/api/v2");
            var pokeApi = MockRestServiceServer.bindTo(builder).build();
            pokeApi.expect(requestTo("https://pokeapi.test/api/v2/pokemon/?offset=0&limit=20"))
                    .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

            var client = new PokeApiClient(builder.build());

            assertThatThrownBy(() -> client.fetchPokemon(0, 20)).isInstanceOf(PokeApiUnavailableException.class);
            pokeApi.verify();
        }

        @Test
        @DisplayName("The one where PokeAPI answers 503 for Bulbasaur's details, so they are reported as unavailable, not as missing")
        void detailsFromPokeApiThatIsDownAreReportedAsUnavailable() {
            var builder = RestClient.builder().baseUrl("https://pokeapi.test/api/v2");
            var pokeApi = MockRestServiceServer.bindTo(builder).build();
            pokeApi.expect(requestTo("https://pokeapi.test/api/v2/pokemon/1/"))
                    .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

            var client = new PokeApiClient(builder.build());

            assertThatThrownBy(() -> client.fetchPokemonDetails(1)).isInstanceOf(PokeApiUnavailableException.class);
            pokeApi.verify();
        }

        @Test
        @DisplayName("The one where PokeAPI answers 503 for its version groups, so the newest one is reported as unavailable")
        void versionGroupsFromPokeApiThatIsDownAreReportedAsUnavailable() {
            var builder = RestClient.builder().baseUrl("https://pokeapi.test/api/v2");
            var pokeApi = MockRestServiceServer.bindTo(builder).build();
            pokeApi.expect(requestTo("https://pokeapi.test/api/v2/version-group/?offset=0&limit=1"))
                    .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

            var client = new PokeApiClient(builder.build());

            assertThatThrownBy(client::fetchNewestVersionGroup).isInstanceOf(PokeApiUnavailableException.class);
            pokeApi.verify();
        }
    }
}