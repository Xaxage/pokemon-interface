package com.xaxage.be.domain.model;

import java.util.List;

/**
 * @param sprite   link to the default front sprite, or {@code null} when the Pokémon has none
 * @param category English genus of the Pokémon's species, or {@code null} when it has none
 * @param weight   weight in hectograms, exactly as PokeAPI gives it, or {@code null} when unknown
 * @param skills   the moves the Pokémon learns, with how it learns each
 */
public record Pokemon(int id, String name, String sprite, String category, Integer weight, List<Skill> skills) {

    public Pokemon(int id, String name) {
        this(id, name, null, null, null, List.of());
    }

    public Pokemon withSprite(String sprite) {
        return new Pokemon(id, name, sprite, category, weight, skills);
    }

    public Pokemon withCategory(String category) {
        return new Pokemon(id, name, sprite, category, weight, skills);
    }

    public Pokemon withWeight(Integer weight) {
        return new Pokemon(id, name, sprite, category, weight, skills);
    }

    public Pokemon withSkills(List<Skill> skills) {
        return new Pokemon(id, name, sprite, category, weight, skills);
    }

    /**
     * @return this Pokémon with only the moves, and the ways of learning them, that apply to the given version group
     */
    public Pokemon learnableIn(String versionGroup) {
        return withSkills(skills.stream()
                .map(skill -> skill.learnableIn(versionGroup))
                .filter(Skill::isLearnable)
                .toList());
    }
}
