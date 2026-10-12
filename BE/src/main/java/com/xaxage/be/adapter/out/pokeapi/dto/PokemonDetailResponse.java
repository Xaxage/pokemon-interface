package com.xaxage.be.adapter.out.pokeapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.xaxage.be.domain.model.*;

import java.util.List;

/**
 * PokeAPI's single {@code /pokemon/{id}} resource.
 */
public record PokemonDetailResponse(int id, String name, Integer weight, Sprites sprites, NamedResource species,
                                    List<Move> moves, List<BaseStat> stats) {

    public Pokemon toPokemon() {
        return new Pokemon(id, name)
                .withSprite(sprites == null ? null : sprites.frontDefault())
                .withWeight(weight)
                .withSkills(moves == null ? List.of() : moves.stream().map(Move::toSkill).toList());
    }

    public DetailedPokemon toDetailedPokemon() {
        var pokemon = new DetailedPokemon(id, name)
                .withStats(stats == null ? List.of() : stats.stream().map(BaseStat::toStat).toList());
        return sprites == null ? pokemon : pokemon.withImage(sprites.officialArtwork(), sprites.frontDefault());
    }

    public record BaseStat(@JsonProperty("base_stat") int baseStat, NamedResource stat) {

        Stat toStat() {
            return new Stat(stat.name(), baseStat);
        }
    }

    public record Sprites(@JsonProperty("front_default") String frontDefault, Other other) {

        String officialArtwork() {
            return other == null || other.officialArtwork() == null ? null : other.officialArtwork().frontDefault();
        }
    }

    public record Other(@JsonProperty("official-artwork") Artwork officialArtwork) {
    }

    public record Artwork(@JsonProperty("front_default") String frontDefault) {
    }

    public record Move(NamedResource move,
                       @JsonProperty("version_group_details") List<VersionGroupDetail> versionGroupDetails) {

        Skill toSkill() {
            return new Skill(move.name(), versionGroupDetails.stream()
                    .map(VersionGroupDetail::toLearnMethod)
                    .toList());
        }
    }

    public record VersionGroupDetail(@JsonProperty("level_learned_at") int levelLearnedAt,
                                     @JsonProperty("move_learn_method") NamedResource moveLearnMethod,
                                     @JsonProperty("version_group") NamedResource versionGroup) {

        private static final String LEVEL_UP = "level-up";

        /**
         * PokeAPI reports level 0 for every method other than levelling up, where a level means nothing.
         */
        LearnMethod toLearnMethod() {
            var method = moveLearnMethod.name();
            return new LearnMethod(method, LEVEL_UP.equals(method) ? levelLearnedAt : null, versionGroup.name());
        }
    }
}
