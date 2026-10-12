package com.xaxage.be.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("A Pokémon")
class PokemonTest {

    @Nested
    @DisplayName("Must show each Pokémon's skills (its moves) and how each is learned, in the newest version group known to PokeAPI")
    class SkillsInTheNewestVersionGroup {

        @Test
        @DisplayName("The one where Tackle is learned by level-up in both Red/Blue and Scarlet/Violet, so only the Scarlet/Violet way is kept")
        void keepsOnlyTheWaysOfLearningInTheGivenVersionGroup() {
            var bulbasaur = new Pokemon(1, "bulbasaur").withSkills(List.of(
                    new Skill("tackle", List.of(
                            new LearnMethod("level-up", 35, "red-blue"),
                            new LearnMethod("level-up", 1, "scarlet-violet")))));

            var learnable = bulbasaur.learnableIn("scarlet-violet");

            assertThat(learnable.skills()).containsExactly(
                    new Skill("tackle", List.of(new LearnMethod("level-up", 1, "scarlet-violet"))));
        }

        @Test
        @DisplayName("The one where Cut could be learned only by machine in Red/Blue, so it is left out")
        void leavesOutMovesLearnableOnlyInOlderGames() {
            var bulbasaur = new Pokemon(1, "bulbasaur").withSkills(List.of(
                    new Skill("tackle", List.of(new LearnMethod("level-up", 1, "scarlet-violet"))),
                    new Skill("cut", List.of(new LearnMethod("machine", null, "red-blue")))));

            var learnable = bulbasaur.learnableIn("scarlet-violet");

            assertThat(learnable.skills()).extracting(Skill::name).containsExactly("tackle");
        }
    }
}
