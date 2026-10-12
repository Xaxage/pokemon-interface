package com.xaxage.be.adapter.in.web;

import com.xaxage.be.adapter.in.web.dto.DetailedPokemonResponse;
import com.xaxage.be.application.port.in.ViewPokemonUseCase;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pokemon")
class PokemonDetailsController {

    private final ViewPokemonUseCase viewPokemon;

    PokemonDetailsController(ViewPokemonUseCase viewPokemon) {
        this.viewPokemon = viewPokemon;
    }

    @GetMapping("/{id}")
    DetailedPokemonResponse view(@PathVariable @Positive int id) {
        return DetailedPokemonResponse.from(viewPokemon.view(id));
    }
}
