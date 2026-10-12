package com.xaxage.be.adapter.in.web;

import com.xaxage.be.adapter.in.web.dto.StoredPokemonPageResponse;
import com.xaxage.be.adapter.in.web.dto.StoredPokemonResponse;
import com.xaxage.be.application.port.in.BrowseStoredPokemonUseCase;
import com.xaxage.be.application.port.in.SynchronisePokemonUseCase;
import com.xaxage.be.domain.model.Synchronisation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stored-pokemon")
class StoredPokemonController {

    private static final int MAX_PAGE_SIZE = 100;

    private final SynchronisePokemonUseCase synchronisePokemon;
    private final BrowseStoredPokemonUseCase browseStoredPokemon;

    StoredPokemonController(SynchronisePokemonUseCase synchronisePokemon,
                            BrowseStoredPokemonUseCase browseStoredPokemon) {
        this.synchronisePokemon = synchronisePokemon;
        this.browseStoredPokemon = browseStoredPokemon;
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

    @GetMapping
    StoredPokemonPageResponse browse(@RequestParam(defaultValue = "0") @Min(0) int page,
                                     @RequestParam(defaultValue = "20") @Min(1) @Max(MAX_PAGE_SIZE) int size) {
        return StoredPokemonPageResponse.from(browseStoredPokemon.browse(page, size), page, size);
    }
}
