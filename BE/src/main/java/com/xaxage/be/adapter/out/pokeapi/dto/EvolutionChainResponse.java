package com.xaxage.be.adapter.out.pokeapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.xaxage.be.domain.model.EvolutionStage;

import java.util.List;

/**
 * PokeAPI's {@code /evolution-chain/{id}} resource.
 *
 * @param chain the chain's first stage
 */
public record EvolutionChainResponse(ChainLink chain) {

    public EvolutionStage toFirstStage() {
        return chain.toStage();
    }

    public record ChainLink(NamedResource species, @JsonProperty("evolves_to") List<ChainLink> evolvesTo) {

        EvolutionStage toStage() {
            return new EvolutionStage(species.name(), evolvesTo.stream().map(ChainLink::toStage).toList());
        }
    }
}
