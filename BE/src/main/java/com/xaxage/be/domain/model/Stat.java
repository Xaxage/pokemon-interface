package com.xaxage.be.domain.model;

/**
 * One of a Pokémon's core stats.
 *
 * @param name the stat, e.g. "hp", "attack" or "special-defense"
 * @param base the Pokémon's base value for the stat
 */
public record Stat(String name, int base) {
}
