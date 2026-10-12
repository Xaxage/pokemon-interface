package com.xaxage.be.adapter.in.web;

import com.xaxage.be.application.port.in.ViewPokemonUseCase;
import com.xaxage.be.domain.exception.PokemonNotFoundException;
import com.xaxage.be.domain.model.DetailedPokemon;
import com.xaxage.be.domain.model.EvolutionStage;
import com.xaxage.be.domain.model.Stat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@WebMvcTest(PokemonDetailsController.class)
@DisplayName("Pokémon detailed view endpoint")
class PokemonDetailsControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ViewPokemonUseCase viewPokemon;

    @Nested
    @DisplayName("Must show a chosen Pokémon by its id")
    class ShowsAChosenPokemonByItsId {

        @Test
        @DisplayName("The one where id 1 is asked for, so Bulbasaur is returned with its id and name")
        void returnsThePokemonWithTheGivenId() {
            given(viewPokemon.view(1)).willReturn(new DetailedPokemon(1, "bulbasaur"));

            var response = mvc.get().uri("/api/pokemon/{id}", 1).exchange();

            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().extractingPath("$.id").isEqualTo(1);
            assertThat(response).bodyJson().extractingPath("$.name").isEqualTo("bulbasaur");
        }

        @Test
        @DisplayName("The one where no Pokémon has the id 99999, so the reader gets a 404 with the standard error body")
        void unknownIdAnswers404() {
            given(viewPokemon.view(99999)).willThrow(new PokemonNotFoundException(99999));

            var response = mvc.get().uri("/api/pokemon/{id}", 99999).exchange();

            assertThat(response).hasStatus(HttpStatus.NOT_FOUND);
            assertThat(response).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
            assertThat(response).bodyJson().extractingPath("$.status").isEqualTo(404);
            assertThat(response).bodyJson().extractingPath("$.title").isEqualTo("Not Found");
            assertThat(response).bodyJson().extractingPath("$.detail").isEqualTo("No Pokémon has the id 99999");
        }
    }

    @Nested
    @DisplayName("Must show the Pokémon's official artwork as its image")
    class ShowsTheOfficialArtworkAsItsImage {

        @Test
        @DisplayName("The one where Bulbasaur comes back with the link to its image")
        void showsTheImageLink() {
            given(viewPokemon.view(1)).willReturn(new DetailedPokemon(1, "bulbasaur").withImage(
                    "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/1.png",
                    "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png"));

            var response = mvc.get().uri("/api/pokemon/{id}", 1).exchange();

            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().extractingPath("$.image")
                    .isEqualTo("https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/1.png");
        }
    }

    @Nested
    @DisplayName("Must show the six core stats with their base values")
    class ShowsTheSixCoreStats {

        @Test
        @DisplayName("The one where Bulbasaur's hp is 45 and its special-attack is 65, so each is shown by name with its base value")
        void showsEachStatWithItsBaseValue() {
            given(viewPokemon.view(1)).willReturn(new DetailedPokemon(1, "bulbasaur")
                    .withStats(List.of(new Stat("hp", 45), new Stat("special-attack", 65))));

            var response = mvc.get().uri("/api/pokemon/{id}", 1).exchange();

            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().extractingPath("$.stats[0].name").isEqualTo("hp");
            assertThat(response).bodyJson().extractingPath("$.stats[0].base").isEqualTo(45);
            assertThat(response).bodyJson().extractingPath("$.stats[1].name").isEqualTo("special-attack");
            assertThat(response).bodyJson().extractingPath("$.stats[1].base").isEqualTo(65);
        }
    }

    @Nested
    @DisplayName("Must show an English description from the Pokémon's species, taken from the newest game that has one")
    class ShowsTheDescription {

        @Test
        @DisplayName("The one where Bulbasaur comes back with its description")
        void showsTheDescription() {
            given(viewPokemon.view(1)).willReturn(new DetailedPokemon(1, "bulbasaur").withDescription(
                    "There is a plant seed on its back right from the day this Pokémon is born. The seed slowly grows larger."));

            var response = mvc.get().uri("/api/pokemon/{id}", 1).exchange();

            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().extractingPath("$.description").isEqualTo(
                    "There is a plant seed on its back right from the day this Pokémon is born. The seed slowly grows larger.");
        }
    }

    @Nested
    @DisplayName("Must show the whole evolution chain, from the first stage to the last")
    class ShowsTheEvolutionChain {

        @Test
        @DisplayName("The one where Ivysaur's chain is Bulbasaur → Ivysaur → Venusaur, so each stage is nested in the one before")
        void showsEachStageNestedInThePreviousOne() {
            given(viewPokemon.view(2)).willReturn(new DetailedPokemon(2, "ivysaur").withEvolutionChain(
                    new EvolutionStage("bulbasaur", List.of(
                            new EvolutionStage("ivysaur", List.of(
                                    new EvolutionStage("venusaur", List.of())))))));

            var response = mvc.get().uri("/api/pokemon/{id}", 2).exchange();

            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().extractingPath("$.evolutionChain.name").isEqualTo("bulbasaur");
            assertThat(response).bodyJson().extractingPath("$.evolutionChain.evolvesTo[0].name").isEqualTo("ivysaur");
            assertThat(response).bodyJson().extractingPath("$.evolutionChain.evolvesTo[0].evolvesTo[0].name")
                    .isEqualTo("venusaur");
            assertThat(response).bodyJson().extractingPath("$.evolutionChain.evolvesTo[0].evolvesTo[0].evolvesTo")
                    .asArray().isEmpty();
        }
    }

    @Nested
    @DisplayName("Must reject an id that is not a positive whole number (400, with the standard error body)")
    class RejectsAnIdThatIsNotAPositiveWholeNumber {

        @ParameterizedTest(name = "The one where id {0} is not positive, so it is rejected")
        @ValueSource(ints = {0, -1})
        @DisplayName("The one where an id that is not positive is rejected with the standard error body")
        void rejectsAnIdThatIsNotPositive(int id) {
            var response = mvc.get().uri("/api/pokemon/{id}", id).exchange();

            assertThat(response).hasStatus(HttpStatus.BAD_REQUEST);
            assertThat(response).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
            assertThat(response).bodyJson().extractingPath("$.status").isEqualTo(400);
            assertThat(response).bodyJson().extractingPath("$.title").isEqualTo("Bad Request");
            then(viewPokemon).shouldHaveNoInteractions();
        }
    }
}
