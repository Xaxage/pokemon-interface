package com.xaxage.be.application.service;

import com.xaxage.be.application.port.out.StoredPokemonRepositoryPort;
import com.xaxage.be.domain.model.DemoPokemon;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@DisplayName("Populating the local database with demo data")
class PopulateDemoDataServiceTest {

    @Nested
    @DisplayName("Must come pre-populated with demo data")
    class ComesPrePopulatedWithDemoData {

        @Test
        @DisplayName("The one where nothing is stored, so the first 10 Pokémon are stored")
        void storesTheDemoPokemonWhenNothingIsStored() {
            var storedPokemon = mock(StoredPokemonRepositoryPort.class);
            given(storedPokemon.isEmpty()).willReturn(true);

            new PopulateDemoDataService(storedPokemon).populate();

            DemoPokemon.FIRST_TEN.forEach(demo -> then(storedPokemon).should().save(demo));
        }

        @Test
        @DisplayName("The one where Pokémon are already stored, so nothing is stored and nothing is overwritten")
        void storesNothingWhenPokemonAreAlreadyStored() {
            var storedPokemon = mock(StoredPokemonRepositoryPort.class);
            given(storedPokemon.isEmpty()).willReturn(false);

            new PopulateDemoDataService(storedPokemon).populate();

            then(storedPokemon).should(never()).save(any());
        }
    }
}
