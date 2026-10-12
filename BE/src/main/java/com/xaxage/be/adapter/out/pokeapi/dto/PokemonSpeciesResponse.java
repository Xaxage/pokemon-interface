package com.xaxage.be.adapter.out.pokeapi.dto;

import java.util.List;

/**
 * PokeAPI's {@code /pokemon-species/{id}} resource.
 */
public record PokemonSpeciesResponse(List<Genus> genera) {

    private static final String ENGLISH = "en";

    /**
     * @return the species' English genus, or {@code null} when it has none
     */
    public String englishGenus() {
        return genera.stream()
                .filter(genus -> ENGLISH.equals(genus.language().name()))
                .map(Genus::genus)
                .findFirst()
                .orElse(null);
    }

    public record Genus(String genus, NamedResource language) {
    }
}
