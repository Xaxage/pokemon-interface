package com.xaxage.be.adapter.out.persistence;

import com.xaxage.be.domain.model.StoredPokemon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs against the project's database, starting each test with nothing stored; every test is rolled back.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(StoredPokemonPersistenceAdapter.class)
@DisplayName("Stored Pokémon persistence")
class StoredPokemonPersistenceAdapterTest {

    @Autowired
    private StoredPokemonPersistenceAdapter adapter;

    @Autowired
    private TestEntityManager entityManager;

    @BeforeEach
    void startWithNothingStored() {
        entityManager.getEntityManager().createNativeQuery("DELETE FROM stored_pokemon_tag").executeUpdate();
        entityManager.getEntityManager().createNativeQuery("DELETE FROM stored_pokemon").executeUpdate();
    }

    @Nested
    @DisplayName("Must keep a stored Pokémon with PokeAPI's fields and our own")
    class KeepsAStoredPokemon {

        @Test
        @DisplayName("The one where Bulbasaur is saved with localized name, region and tags, so it reads back the same")
        void savedPokemonReadsBackTheSame() {
            var bulbasaur = new StoredPokemon(1, "bulbasaur", "Seed Pokémon", 69, "bulbasaur.png",
                    "Bisasam", "Kanto", List.of("starter", "grass"));

            var saved = adapter.save(bulbasaur);
            entityManager.flush();
            entityManager.clear();

            assertThat(saved).isEqualTo(bulbasaur);
            assertThat(entityManager.find(StoredPokemonEntity.class, 1).toStoredPokemon()).isEqualTo(bulbasaur);
        }
    }

    @Nested
    @DisplayName("Must find a stored Pokémon by its id")
    class FindsAStoredPokemonByItsId {

        @Test
        @DisplayName("The one where Bulbasaur is stored, so looking up id 1 finds it with all its fields")
        void findsTheStoredPokemon() {
            var bulbasaur = new StoredPokemon(1, "bulbasaur", "Seed Pokémon", 69, "bulbasaur.png",
                    "Bisasam", "Kanto", List.of("starter"));
            entityManager.persistAndFlush(StoredPokemonEntity.from(bulbasaur));
            entityManager.clear();

            assertThat(adapter.findById(1)).contains(bulbasaur);
        }

        @Test
        @DisplayName("The one where id 99999 is not stored, so nothing is found")
        void findsNothingForAnIdThatIsNotStored() {
            assertThat(adapter.findById(99999)).isEmpty();
        }
    }
}
