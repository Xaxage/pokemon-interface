package com.xaxage.be.adapter.out.pokeapi;

import com.github.benmanes.caffeine.cache.Ticker;
import com.xaxage.be.application.port.out.PokeApiPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@SpringJUnitConfig(PokeApiCachingTest.CachedPokeApiClient.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("Caching PokeAPI responses")
class PokeApiCachingTest {

    @Autowired
    private PokeApiPort pokeApi;

    @Autowired
    private MockRestServiceServer server;

    @Autowired
    private FakeTicker clock;

    @Nested
    @DisplayName("Should reuse PokeAPI responses for 24 hours instead of asking again")
    class ReusesResponsesFor24Hours {

        @Test
        @DisplayName("The one where page 0 of 20 is asked for twice, so PokeAPI's list is fetched only once")
        void reusesTheListOfAPage() {
            server.expect(ExpectedCount.once(), requestTo("https://pokeapi.test/api/v2/pokemon/?offset=0&limit=20"))
                    .andRespond(withSuccess("""
                            {"count": 1, "next": null, "results": [
                              {"name": "bulbasaur", "url": "https://pokeapi.test/api/v2/pokemon/1/"}
                            ]}
                            """, MediaType.APPLICATION_JSON));

            var first = pokeApi.fetchPokemon(0, 20);
            var second = pokeApi.fetchPokemon(0, 20);

            assertThat(second).isEqualTo(first);
            server.verify();
        }

        @Test
        @DisplayName("The one where more than 24 hours pass between two asks for page 0 of 20, so PokeAPI's list is fetched again")
        void asksAgainAfter24Hours() {
            server.expect(ExpectedCount.twice(), requestTo("https://pokeapi.test/api/v2/pokemon/?offset=0&limit=20"))
                    .andRespond(withSuccess("""
                            {"count": 1, "next": null, "results": [
                              {"name": "bulbasaur", "url": "https://pokeapi.test/api/v2/pokemon/1/"}
                            ]}
                            """, MediaType.APPLICATION_JSON));

            pokeApi.fetchPokemon(0, 20);
            clock.advance(Duration.ofHours(24).plusMinutes(1));
            pokeApi.fetchPokemon(0, 20);

            server.verify();
        }

        @Test
        @DisplayName("The one where Bulbasaur's details are asked for twice, so PokeAPI's Pokémon and species are fetched only once")
        void reusesThePokemonsDetails() {
            server.expect(ExpectedCount.once(), requestTo("https://pokeapi.test/api/v2/pokemon/1/"))
                    .andRespond(withSuccess("""
                            {"id": 1, "name": "bulbasaur", "weight": 69,
                             "species": {"name": "bulbasaur", "url": "https://pokeapi.test/api/v2/pokemon-species/1/"}}
                            """, MediaType.APPLICATION_JSON));
            server.expect(ExpectedCount.once(), requestTo("https://pokeapi.test/api/v2/pokemon-species/1/"))
                    .andRespond(withSuccess("""
                            {"genera": [{"genus": "Seed Pokémon", "language": {"name": "en", "url": "https://pokeapi.test/api/v2/language/9/"}}]}
                            """, MediaType.APPLICATION_JSON));

            var first = pokeApi.fetchPokemonDetails(1);
            var second = pokeApi.fetchPokemonDetails(1);

            assertThat(second).isEqualTo(first);
            server.verify();
        }

        @Test
        @DisplayName("The one where the newest version group is asked for twice, so PokeAPI's version groups are fetched only once")
        void reusesTheNewestVersionGroup() {
            server.expect(ExpectedCount.once(), requestTo("https://pokeapi.test/api/v2/version-group/?offset=0&limit=1"))
                    .andRespond(withSuccess("""
                            {"count": 2, "results": [{"name": "red-blue", "url": "https://pokeapi.test/api/v2/version-group/1/"}]}
                            """, MediaType.APPLICATION_JSON));
            server.expect(ExpectedCount.once(), requestTo("https://pokeapi.test/api/v2/version-group/?offset=1&limit=1"))
                    .andRespond(withSuccess("""
                            {"count": 2, "results": [{"name": "scarlet-violet", "url": "https://pokeapi.test/api/v2/version-group/25/"}]}
                            """, MediaType.APPLICATION_JSON));

            var first = pokeApi.fetchNewestVersionGroup();
            var second = pokeApi.fetchNewestVersionGroup();

            assertThat(second).isEqualTo(first).isEqualTo("scarlet-violet");
            server.verify();
        }
    }

    @Nested
    @DisplayName("Should reuse PokeAPI responses for 24 hours and keep serving them while PokeAPI is unavailable")
    class ReusesTheDetailedViewFor24Hours {

        @Test
        @DisplayName("The one where Bulbasaur is viewed twice, so PokeAPI's Pokémon, species and evolution chain are fetched only once")
        void reusesTheDetailedPokemon() {
            server.expect(ExpectedCount.once(), requestTo("https://pokeapi.test/api/v2/pokemon/1/"))
                    .andRespond(withSuccess("""
                            {"id": 1, "name": "bulbasaur",
                             "species": {"name": "bulbasaur", "url": "https://pokeapi.test/api/v2/pokemon-species/1/"}}
                            """, MediaType.APPLICATION_JSON));
            server.expect(ExpectedCount.once(), requestTo("https://pokeapi.test/api/v2/pokemon-species/1/"))
                    .andRespond(withSuccess("""
                            {"flavor_text_entries": [],
                             "evolution_chain": {"url": "https://pokeapi.test/api/v2/evolution-chain/1/"}}
                            """, MediaType.APPLICATION_JSON));
            server.expect(ExpectedCount.once(), requestTo("https://pokeapi.test/api/v2/evolution-chain/1/"))
                    .andRespond(withSuccess("""
                            {"chain": {"species": {"name": "bulbasaur", "url": "https://pokeapi.test/api/v2/pokemon-species/1/"},
                                       "evolves_to": []}}
                            """, MediaType.APPLICATION_JSON));

            var first = pokeApi.fetchDetailedPokemon(1);
            var second = pokeApi.fetchDetailedPokemon(1);

            assertThat(second).isEqualTo(first);
            server.verify();
        }
    }

    /**
     * The real client and its cache, talking to a mocked PokeAPI on a clock the test controls.
     */
    @TestConfiguration(proxyBeanMethods = false)
    @Import({PokeApiClient.class, PokeApiCacheConfig.class})
    static class CachedPokeApiClient {

        private final RestClient.Builder builder = RestClient.builder().baseUrl("https://pokeapi.test/api/v2");

        @Bean
        MockRestServiceServer server() {
            return MockRestServiceServer.bindTo(builder).build();
        }

        @Bean(PokeApiConfig.POKE_API_REST_CLIENT)
        RestClient pokeApiRestClient(MockRestServiceServer server) {
            return builder.build();
        }

        @Bean
        @Primary
        FakeTicker fakeTicker() {
            return new FakeTicker();
        }
    }

    static final class FakeTicker implements Ticker {

        private final AtomicLong nanos = new AtomicLong();

        @Override
        public long read() {
            return nanos.get();
        }

        void advance(Duration duration) {
            nanos.addAndGet(duration.toNanos());
        }
    }
}
