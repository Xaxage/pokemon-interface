package com.xaxage.be.adapter.in.web;

import com.xaxage.be.adapter.in.web.dto.StoredPokemonResponse;
import com.xaxage.be.application.port.in.SynchronisePokemonUseCase;
import com.xaxage.be.domain.model.Synchronisation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stored-pokemon")
class StoredPokemonController {

    private final SynchronisePokemonUseCase synchronisePokemon;

    StoredPokemonController(SynchronisePokemonUseCase synchronisePokemon) {
        this.synchronisePokemon = synchronisePokemon;
    }

    @PostMapping("/{id}/sync")
    ResponseEntity<StoredPokemonResponse> synchronise(@PathVariable int id) {
        var synchronisation = synchronisePokemon.synchronise(id);
        var status = switch (synchronisation) {
            case Synchronisation.Copied copied -> HttpStatus.CREATED;
            case Synchronisation.Refreshed refreshed -> HttpStatus.OK;
        };
        return ResponseEntity.status(status).body(StoredPokemonResponse.from(synchronisation.pokemon()));
    }
}
