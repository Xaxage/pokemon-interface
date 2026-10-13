package com.xaxage.be.application.service;

import com.xaxage.be.application.port.out.StoredPokemonRepositoryPort;
import com.xaxage.be.domain.model.StoredPokemon;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@DisplayName("Editing our own fields of a Pokémon stored in the local database")
class EditStoredPokemonServiceTest {

    @Nested
    @DisplayName("Must replace our own fields of a stored Pokémon with the ones sent")
    class ReplacesOurOwnFields {

        @Test
        @DisplayName("The one where stored Bulbasaur is edited with \"Bisasam\", \"Kanto\" and tags \"starter\" and "
                + "\"grass\", so the edited copy is stored and returned")
        void storesAndReturnsTheEditedCopy() {
            var storedPokemon = mock(StoredPokemonRepositoryPort.class);
            given(storedPokemon.findById(1)).willReturn(Optional.of(new StoredPokemon(1, "bulbasaur",
                    "Seed Pokémon", 69, "bulbasaur.png", null, null, List.of())));
            given(storedPokemon.save(any())).willAnswer(invocation -> invocation.getArgument(0));

            var edited = new EditStoredPokemonService(storedPokemon)
                    .edit(1, "Bisasam", "Kanto", List.of("starter", "grass"));

            var expected = new StoredPokemon(1, "bulbasaur", "Seed Pokémon", 69, "bulbasaur.png",
                    "Bisasam", "Kanto", List.of("starter", "grass"));
            assertThat(edited).isEqualTo(expected);
            then(storedPokemon).should().save(expected);
        }
    }
}
