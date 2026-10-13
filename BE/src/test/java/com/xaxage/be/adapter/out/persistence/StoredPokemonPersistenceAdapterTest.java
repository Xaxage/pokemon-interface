package com.xaxage.be.adapter.out.persistence;

import com.xaxage.be.domain.model.StoredPokemon;
import com.xaxage.be.domain.model.StoredPokemonPage;
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
 * Runs against the in-memory test database, starting each test with nothing stored; every test is rolled back.
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

    @Nested
    @DisplayName("Must list stored Pokémon one page at a time, ordered by id")
    class ListsStoredPokemonOnePageAtATime {

        @Test
        @DisplayName("The one where Squirtle, Bulbasaur and Charmander are stored in that order, so page 0 of size 2 is "
                + "Bulbasaur then Charmander, out of 3")
        void pageIsOrderedByIdWithTheTotal() {
            var squirtle = new StoredPokemon(7, "squirtle", "Tiny Turtle Pokémon", 90, "squirtle.png",
                    null, null, List.of());
            var bulbasaur = new StoredPokemon(1, "bulbasaur", "Seed Pokémon", 69, "bulbasaur.png",
                    null, null, List.of());
            var charmander = new StoredPokemon(4, "charmander", "Lizard Pokémon", 85, "charmander.png",
                    null, null, List.of());
            entityManager.persist(StoredPokemonEntity.from(squirtle));
            entityManager.persist(StoredPokemonEntity.from(bulbasaur));
            entityManager.persist(StoredPokemonEntity.from(charmander));
            entityManager.flush();
            entityManager.clear();

            assertThat(adapter.findPage(0, 2)).isEqualTo(new StoredPokemonPage(List.of(bulbasaur, charmander), 3));
        }
    }

    @Nested
    @DisplayName("Must delete a stored Pokémon")
    class DeletesAStoredPokemon {

        @Test
        @DisplayName("The one where Bulbasaur is stored with tags \"starter\" and \"grass\", so deleting it leaves neither "
                + "Bulbasaur nor its tags stored")
        void deletingRemovesThePokemonAndItsTags() {
            entityManager.persistAndFlush(StoredPokemonEntity.from(new StoredPokemon(1, "bulbasaur", "Seed Pokémon",
                    69, "bulbasaur.png", "Bisasam", "Kanto", List.of("starter", "grass"))));
            entityManager.clear();

            adapter.delete(1);
            entityManager.flush();
            entityManager.clear();

            assertThat(adapter.findById(1)).isEmpty();
            assertThat(entityManager.getEntityManager()
                    .createNativeQuery("SELECT COUNT(*) FROM stored_pokemon_tag WHERE pokemon_id = 1")
                    .getSingleResult())
                    .isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("Must tell whether any Pokémon is stored")
    class TellsWhetherAnyPokemonIsStored {

        @Test
        @DisplayName("The one where nothing is stored, so the local database is empty")
        void nothingStoredIsEmpty() {
            assertThat(adapter.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("The one where Bulbasaur is stored, so the local database is not empty")
        void somethingStoredIsNotEmpty() {
            entityManager.persistAndFlush(StoredPokemonEntity.from(new StoredPokemon(1, "bulbasaur", "Seed Pokémon",
                    69, "bulbasaur.png", null, null, List.of())));

            assertThat(adapter.isEmpty()).isFalse();
        }
    }
}
