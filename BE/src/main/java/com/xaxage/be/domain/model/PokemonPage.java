package com.xaxage.be.domain.model;

import java.util.List;

public record PokemonPage(List<Pokemon> content, boolean hasNext) {
}
