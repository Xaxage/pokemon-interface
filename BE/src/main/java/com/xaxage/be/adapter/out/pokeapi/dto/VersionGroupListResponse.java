package com.xaxage.be.adapter.out.pokeapi.dto;

import java.util.List;

/**
 * PokeAPI's paginated {@code /version-group} list, oldest first.
 */
public record VersionGroupListResponse(int count, List<NamedResource> results) {
}
