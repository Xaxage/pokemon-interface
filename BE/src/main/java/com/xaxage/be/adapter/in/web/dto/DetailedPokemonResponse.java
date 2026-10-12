package com.xaxage.be.adapter.in.web.dto;

import com.xaxage.be.domain.model.DetailedPokemon;
import com.xaxage.be.domain.model.EvolutionStage;
import com.xaxage.be.domain.model.Stat;

import java.util.List;

public record DetailedPokemonResponse(int id, String name, String image, List<StatResponse> stats,
                                      String description, EvolutionStageResponse evolutionChain) {

    public static DetailedPokemonResponse from(DetailedPokemon pokemon) {
        return new DetailedPokemonResponse(pokemon.id(), pokemon.name(), pokemon.image(),
                pokemon.stats().stream().map(StatResponse::from).toList(), pokemon.description(),
                pokemon.evolutionChain() == null ? null : EvolutionStageResponse.from(pokemon.evolutionChain()));
    }

    public record StatResponse(String name, int base) {

        static StatResponse from(Stat stat) {
            return new StatResponse(stat.name(), stat.base());
        }
    }

    public record EvolutionStageResponse(String name, List<EvolutionStageResponse> evolvesTo) {

        static EvolutionStageResponse from(EvolutionStage stage) {
            return new EvolutionStageResponse(stage.name(),
                    stage.evolvesTo().stream().map(EvolutionStageResponse::from).toList());
        }
    }
}
