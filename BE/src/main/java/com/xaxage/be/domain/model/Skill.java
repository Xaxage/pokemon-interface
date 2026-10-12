package com.xaxage.be.domain.model;

import java.util.List;

/**
 * A move the Pokémon can use in battle, with every way it learns it.
 */
public record Skill(String name, List<LearnMethod> methods) {

    Skill learnableIn(String versionGroup) {
        return new Skill(name, methods.stream()
                .filter(method -> method.versionGroup().equals(versionGroup))
                .toList());
    }

    boolean isLearnable() {
        return !methods.isEmpty();
    }
}
