package com.xaxage.be.domain.model;

import java.util.List;

/**
 * One Pokémon as shown in its detailed view.
 *
 * @param image          link to the Pokémon's official artwork, or to its default front sprite when it has no artwork
 * @param stats          the Pokémon's core stats with their base values
 * @param description    English description of the Pokémon's species, or {@code null} when it has none
 * @param evolutionChain the whole evolution chain the Pokémon belongs to, from its first stage
 */
public record DetailedPokemon(int id, String name, String image, List<Stat> stats, String description,
                              EvolutionStage evolutionChain) {

    public DetailedPokemon(int id, String name) {
        this(id, name, null, List.of(), null, null);
    }

    public DetailedPokemon withImage(String officialArtwork, String defaultSprite) {
        return new DetailedPokemon(id, name, officialArtwork != null ? officialArtwork : defaultSprite, stats,
                description, evolutionChain);
    }

    public DetailedPokemon withStats(List<Stat> stats) {
        return new DetailedPokemon(id, name, image, stats, description, evolutionChain);
    }

    /**
     * @param flavorText the species' description as PokeAPI gives it, broken over lines and pages for a game screen,
     *                   or {@code null} when it has none
     */
    public DetailedPokemon withDescription(String flavorText) {
        return new DetailedPokemon(id, name, image, stats, flavorText == null ? null : asSingleLine(flavorText),
                evolutionChain);
    }

    public DetailedPokemon withEvolutionChain(EvolutionStage firstStage) {
        return new DetailedPokemon(id, name, image, stats, description, firstStage);
    }

    private static String asSingleLine(String flavorText) {
        return flavorText.replaceAll("[\n\f]", " ");
    }
}
