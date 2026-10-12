package com.xaxage.be.domain.model;

import java.util.List;

/**
 * One stage of an evolution chain, with every stage it can evolve into next.
 *
 * @param name      the species at this stage
 * @param evolvesTo the next stages; more than one when the chain branches, none at the last stage
 */
public record EvolutionStage(String name, List<EvolutionStage> evolvesTo) {
}
