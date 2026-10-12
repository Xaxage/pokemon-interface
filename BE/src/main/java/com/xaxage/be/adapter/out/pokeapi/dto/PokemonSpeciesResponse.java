package com.xaxage.be.adapter.out.pokeapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Comparator;
import java.util.List;

/**
 * PokeAPI's {@code /pokemon-species/{id}} resource.
 *
 * @param evolutionChain link to the evolution chain the species belongs to; PokeAPI gives it a url but no name
 */
public record PokemonSpeciesResponse(List<Genus> genera,
                                     @JsonProperty("flavor_text_entries") List<FlavorText> flavorTextEntries,
                                     @JsonProperty("evolution_chain") NamedResource evolutionChain) {

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

    /**
     * PokeAPI numbers games (versions) in the order they came out, so the newest has the highest id.
     *
     * @return the species' English description from the newest game that has one, exactly as PokeAPI gives it
     */
    public String newestEnglishFlavorText() {
        return flavorTextEntries.stream()
                .filter(entry -> ENGLISH.equals(entry.language().name()))
                .max(Comparator.comparingInt(entry -> entry.version().id()))
                .map(FlavorText::flavorText)
                .orElse(null);
    }

    public record Genus(String genus, NamedResource language) {
    }

    public record FlavorText(@JsonProperty("flavor_text") String flavorText, NamedResource language,
                             NamedResource version) {
    }
}
