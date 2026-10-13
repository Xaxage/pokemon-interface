package com.xaxage.be.application.service;

import com.xaxage.be.application.port.out.StoredPokemonRepositoryPort;
import com.xaxage.be.domain.model.StoredPokemon;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@DisplayName("Deleting a Pokémon stored in the local database")
class DeleteStoredPokemonServiceTest {

    @Nested
    @DisplayName("Must delete a stored Pokémon")
    class DeletesAStoredPokemon {

        @Test
        @DisplayName("The one where Bulbasaur is stored, so deleting it removes it from the local database")
        void removesTheStoredPokemon() {
            var storedPokemon = mock(StoredPokemonRepositoryPort.class);
            given(storedPokemon.findById(1)).willReturn(Optional.of(new StoredPokemon(1, "bulbasaur",
                    "Seed Pokémon", 69, "bulbasaur.png", null, null, List.of())));

            new DeleteStoredPokemonService(storedPokemon).delete(1);

            then(storedPokemon).should().delete(1);
        }
    }
}
