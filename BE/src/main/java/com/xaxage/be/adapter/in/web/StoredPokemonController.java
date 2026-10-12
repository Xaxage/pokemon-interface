package com.xaxage.be.adapter.in.web;

import com.xaxage.be.adapter.in.web.dto.StoredPokemonResponse;
import com.xaxage.be.application.port.in.SynchronisePokemonUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stored-pokemon")
class StoredPokemonController {

    private final SynchronisePokemonUseCase synchronisePokemon;

    StoredPokemonController(SynchronisePokemonUseCase synchronisePokemon) {
        this.synchronisePokemon = synchronisePokemon;
    }

    @PostMapping("/{id}/sync")
    @ResponseStatus(HttpStatus.CREATED)
    StoredPokemonResponse synchronise(@PathVariable int id) {
        return StoredPokemonResponse.from(synchronisePokemon.synchronise(id));
    }
}
