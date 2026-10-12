package com.xaxage.be.adapter.in.web;

import com.xaxage.be.adapter.in.web.dto.PokemonPageResponse;
import com.xaxage.be.application.port.in.BrowsePokemonUseCase;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pokemon")
class PokemonController {

    private static final int MAX_PAGE_SIZE = 100;

    private final BrowsePokemonUseCase browsePokemon;

    PokemonController(BrowsePokemonUseCase browsePokemon) {
        this.browsePokemon = browsePokemon;
    }

    @GetMapping
    PokemonPageResponse browse(@RequestParam(defaultValue = "0") @Min(0) int page,
                               @RequestParam(defaultValue = "20") @Min(1) @Max(MAX_PAGE_SIZE) int size) {
        return PokemonPageResponse.from(browsePokemon.browse(page, size), page, size);
    }
}
