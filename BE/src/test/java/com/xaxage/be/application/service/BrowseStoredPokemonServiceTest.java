package com.xaxage.be.application.service;

import com.xaxage.be.application.port.out.StoredPokemonRepositoryPort;
import com.xaxage.be.domain.model.StoredPokemon;
import com.xaxage.be.domain.model.StoredPokemonPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@DisplayName("Browsing the Pokémon stored in the local database")
class BrowseStoredPokemonServiceTest {

    @Nested
    @DisplayName("Must list stored Pokémon one page at a time, ordered by id")
    class ListsStoredPokemonOnePageAtATime {

        @Test
        @DisplayName("The one where page 0 of size 20 is asked for, so the local database's page 0 of size 20 is returned")
        void returnsThePageTheLocalDatabaseHas() {
            var storedPokemon = mock(StoredPokemonRepositoryPort.class);
            var page = new StoredPokemonPage(List.of(
                    new StoredPokemon(1, "bulbasaur", "Seed Pokémon", 69, "bulbasaur.png", null, null, List.of())), 1);
            given(storedPokemon.findPage(0, 20)).willReturn(page);

            var browsed = new BrowseStoredPokemonService(storedPokemon).browse(0, 20);

            assertThat(browsed).isEqualTo(page);
        }
    }
}
