package com.xaxage.be.adapter.in.web;

import com.xaxage.be.application.port.in.BrowsePokemonUseCase;
import com.xaxage.be.application.port.out.PokeApiUnavailableException;
import com.xaxage.be.domain.model.LearnMethod;
import com.xaxage.be.domain.model.Pokemon;
import com.xaxage.be.domain.model.PokemonPage;
import com.xaxage.be.domain.model.Skill;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@WebMvcTest(PokemonController.class)
@Import(SecurityConfig.class)
@DisplayName("Pokémon browse endpoint")
class PokemonControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private BrowsePokemonUseCase browsePokemon;

    @Nested
    @DisplayName("Browsing a page of Pokémon")
    class BrowsingAPage {

        @Test
        @DisplayName("The one where no page or size is given, so page 0 of 20 is asked for and returned as-is")
        void defaultsToFirstPageOfTwenty() {
            given(browsePokemon.browse(0, 20))
                    .willReturn(new PokemonPage(List.of(new Pokemon(1, "bulbasaur")), true, 1350));

            var response = mvc.get().uri("/api/pokemon").exchange();

            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().extractingPath("$.content[0].id").isEqualTo(1);
            assertThat(response).bodyJson().extractingPath("$.content[0].name").isEqualTo("bulbasaur");
            assertThat(response).bodyJson().extractingPath("$.hasNext").isEqualTo(true);
        }

        @Test
        @DisplayName("The one where a page past the last page comes back with an empty list of entries and no next page")
        void pagePastTheLastHasEmptyContent() {
            given(browsePokemon.browse(2, 20))
                    .willReturn(new PokemonPage(List.of(), false, 1350));

            var response = mvc.get().uri("/api/pokemon").param("page", "2").param("size", "20").exchange();

            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().extractingPath("$.content").asArray().isEmpty();
            assertThat(response).bodyJson().extractingPath("$.hasNext").isEqualTo(false);
        }
    }

    @Nested
    @DisplayName("Must tell the reader where they are in the full list")
    class WhereTheReaderIsInTheFullList {

        @Test
        @DisplayName("The one where page 1 of size 20 out of 45 Pokémon reports page 1, size 20, a count of 45 and 3 pages")
        void reportsPagePositionAndTotals() {
            given(browsePokemon.browse(1, 20))
                    .willReturn(new PokemonPage(List.of(new Pokemon(21, "spearow")), true, 45));

            var response = mvc.get().uri("/api/pokemon").param("page", "1").param("size", "20").exchange();

            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().extractingPath("$.page").isEqualTo(1);
            assertThat(response).bodyJson().extractingPath("$.size").isEqualTo(20);
            assertThat(response).bodyJson().extractingPath("$.count").isEqualTo(45);
            assertThat(response).bodyJson().extractingPath("$.totalPages").isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Must show each Pokémon's default front sprite")
    class ShowsEachPokemonsDefaultFrontSprite {

        @Test
        @DisplayName("The one where Bulbasaur comes back with the link to its default front battle sprite")
        void showsTheSpriteLink() {
            given(browsePokemon.browse(0, 20))
                    .willReturn(new PokemonPage(List.of(new Pokemon(1, "bulbasaur")
                            .withSprite("https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png")), false, 1));

            var response = mvc.get().uri("/api/pokemon").exchange();

            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().extractingPath("$.content[0].sprite")
                    .isEqualTo("https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png");
        }
    }

    @Nested
    @DisplayName("Must show each Pokémon's category as its English species genus")
    class ShowsEachPokemonsCategory {

        @Test
        @DisplayName("The one where Bulbasaur's category is \"Seed Pokémon\"")
        void showsTheCategory() {
            given(browsePokemon.browse(0, 20))
                    .willReturn(new PokemonPage(List.of(new Pokemon(1, "bulbasaur").withCategory("Seed Pokémon")),
                            false, 1));

            var response = mvc.get().uri("/api/pokemon").exchange();

            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().extractingPath("$.content[0].category").isEqualTo("Seed Pokémon");
        }
    }

    @Nested
    @DisplayName("Must show each Pokémon's weight in hectograms, exactly as PokeAPI gives it")
    class ShowsEachPokemonsWeight {

        @Test
        @DisplayName("The one where Bulbasaur is shown weighing 69 hg")
        void showsTheWeightInHectograms() {
            given(browsePokemon.browse(0, 20))
                    .willReturn(new PokemonPage(List.of(new Pokemon(1, "bulbasaur").withWeight(69)), false, 1));

            var response = mvc.get().uri("/api/pokemon").exchange();

            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().extractingPath("$.content[0].weight").isEqualTo(69);
        }
    }

    @Nested
    @DisplayName("Must show each Pokémon's skills (its moves) and how each is learned, in the newest version group known to PokeAPI")
    class ShowsEachPokemonsSkills {

        @Test
        @DisplayName("The one where Take Down is learned by level-up at 15 and by machine, so it is shown once with both methods")
        void showsEachSkillOnceWithEveryWayItIsLearned() {
            given(browsePokemon.browse(0, 20))
                    .willReturn(new PokemonPage(List.of(new Pokemon(1, "bulbasaur").withSkills(List.of(
                            new Skill("take-down", List.of(
                                    new LearnMethod("level-up", 15, "scarlet-violet"),
                                    new LearnMethod("machine", null, "scarlet-violet")))))), false, 1));

            var response = mvc.get().uri("/api/pokemon").exchange();

            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().extractingPath("$.content[0].skills[0].name").isEqualTo("take-down");
            assertThat(response).bodyJson().extractingPath("$.content[0].skills[0].methods[0].method")
                    .isEqualTo("level-up");
            assertThat(response).bodyJson().extractingPath("$.content[0].skills[0].methods[0].level").isEqualTo(15);
            assertThat(response).bodyJson().extractingPath("$.content[0].skills[0].methods[1].method")
                    .isEqualTo("machine");
            assertThat(response).bodyJson().hasPath("$.content[0].skills[0].methods[1].level");
            assertThat(response).bodyJson().extractingPath("$.content[0].skills[0].methods[1].level").isNull();
            assertThat(response).bodyJson().doesNotHavePath("$.content[0].skills[0].methods[0].versionGroup");
        }
    }

    @Nested
    @DisplayName("Must reject pagination requests that are not valid (400, with the standard error body)")
    class RejectsPaginationThatIsNotValid {

        @ParameterizedTest(name = "The one where page {0} of size {1} is out of range, so it is rejected")
        @CsvSource({
                "-1, 20",
                "0,  0",
                "0,  101"
        })
        @DisplayName("The one where a page or size out of range is rejected with the standard error body")
        void rejectsPageOrSizeOutOfRange(String page, String size) {
            var response = mvc.get().uri("/api/pokemon").param("page", page).param("size", size).exchange();

            assertThat(response).hasStatus(HttpStatus.BAD_REQUEST);
            assertThat(response).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
            assertThat(response).bodyJson().extractingPath("$.status").isEqualTo(400);
            assertThat(response).bodyJson().extractingPath("$.title").isEqualTo("Bad Request");
            then(browsePokemon).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("Must keep serving cached data while PokeAPI is unavailable, and otherwise fail clearly")
    class FailsClearlyWhenPokeApiIsUnavailable {

        @Test
        @DisplayName("The one where PokeAPI is unavailable, so the reader gets a 503 with the standard error body and no upstream details")
        void answers503WithoutUpstreamDetails() throws Exception {
            given(browsePokemon.browse(0, 20)).willThrow(new PokeApiUnavailableException(
                    new IllegalStateException("upstream pokeapi-db-7.internal:5432 refused connection")));

            var response = mvc.get().uri("/api/pokemon").exchange();

            assertThat(response).hasStatus(HttpStatus.SERVICE_UNAVAILABLE);
            assertThat(response).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
            assertThat(response).bodyJson().extractingPath("$.status").isEqualTo(503);
            assertThat(response).bodyJson().extractingPath("$.title").isEqualTo("Service Unavailable");
            assertThat(response).bodyJson().extractingPath("$.detail")
                    .isEqualTo("Pokémon data is temporarily unavailable. Please try again later.");
            assertThat(response.getResponse().getContentAsString()).doesNotContain("pokeapi-db-7", "Exception");
        }
    }
}