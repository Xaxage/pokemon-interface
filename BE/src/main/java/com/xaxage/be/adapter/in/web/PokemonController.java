package com.xaxage.be.adapter.in.web;

import com.xaxage.be.adapter.in.web.dto.PokemonPageResponse;
import com.xaxage.be.application.port.in.BrowsePokemonUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pokemon")
class PokemonController {

    private final BrowsePokemonUseCase browsePokemon;

    PokemonController(BrowsePokemonUseCase browsePokemon) {
        this.browsePokemon = browsePokemon;
    }

    @GetMapping
    PokemonPageResponse browse(@RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "20") int size) {
        return PokemonPageResponse.from(browsePokemon.browse(page, size));
    }
}