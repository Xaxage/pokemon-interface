package com.xaxage.be.application.service;

import com.xaxage.be.application.port.out.StoredPokemonRepositoryPort;
import com.xaxage.be.domain.exception.PokemonNotStoredException;
import com.xaxage.be.domain.model.StoredPokemon;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

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

    @Nested
    @DisplayName("Must answer 404 when the Pokémon is not stored locally")
    class ReportsAPokemonThatIsNotStored {

        @Test
        @DisplayName("The one where id 99999 is not stored, so it is reported as not stored and nothing is deleted")
        void pokemonNotStoredIsReportedAndNothingIsDeleted() {
            var storedPokemon = mock(StoredPokemonRepositoryPort.class);
            given(storedPokemon.findById(99999)).willReturn(Optional.empty());

            var service = new DeleteStoredPokemonService(storedPokemon);

            assertThatThrownBy(() -> service.delete(99999))
                    .isInstanceOf(PokemonNotStoredException.class)
                    .hasMessage("No Pokémon with the id 99999 is stored");
            then(storedPokemon).should(never()).delete(anyInt());
        }
    }
}
