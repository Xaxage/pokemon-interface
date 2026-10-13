package com.xaxage.be.adapter.in.web.dto;

import java.util.List;

/**
 * Our own fields of a stored Pokémon, as the reader wants them; any left out are cleared.
 */
public record EditStoredPokemonRequest(String localizedName, String region, List<String> tags) {
}
