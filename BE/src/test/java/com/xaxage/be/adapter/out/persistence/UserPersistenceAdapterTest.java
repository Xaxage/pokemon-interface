package com.xaxage.be.adapter.out.persistence;

import com.xaxage.be.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs against the in-memory test database, starting each test with no users; every test is rolled back.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(UserPersistenceAdapter.class)
@DisplayName("User persistence")
class UserPersistenceAdapterTest {

    @Autowired
    private UserPersistenceAdapter adapter;

    @Autowired
    private TestEntityManager entityManager;

    @BeforeEach
    void startWithNoUsers() {
        entityManager.getEntityManager().createNativeQuery("DELETE FROM app_user").executeUpdate();
    }

    @Nested
    @DisplayName("Must keep a registered user")
    class KeepsARegisteredUser {

        @Test
        @DisplayName("The one where ash is saved, so ash reads back the same")
        void savedUserReadsBackTheSame() {
            var ash = new User("ash", "hash-of-pikachu123");

            var saved = adapter.save(ash);
            entityManager.flush();
            entityManager.clear();

            assertThat(saved).isEqualTo(ash);
            assertThat(entityManager.find(UserEntity.class, "ash").toUser()).isEqualTo(ash);
        }
    }

    @Nested
    @DisplayName("Must tell whether a username is taken")
    class TellsWhetherAUsernameIsTaken {

        @Test
        @DisplayName("The one where ash is registered and misty is not, so only ash is taken")
        void onlyARegisteredUsernameIsTaken() {
            entityManager.persistAndFlush(UserEntity.from(new User("ash", "hash-of-pikachu123")));

            assertThat(adapter.existsByUsername("ash")).isTrue();
            assertThat(adapter.existsByUsername("misty")).isFalse();
        }
    }

    @Nested
    @DisplayName("Must find a registered user by username")
    class FindsARegisteredUser {

        @Test
        @DisplayName("The one where ash is registered, so ash is found and misty is not")
        void onlyARegisteredUserIsFound() {
            entityManager.persistAndFlush(UserEntity.from(new User("ash", "hash-of-pikachu123")));
            entityManager.clear();

            assertThat(adapter.findByUsername("ash")).contains(new User("ash", "hash-of-pikachu123"));
            assertThat(adapter.findByUsername("misty")).isEmpty();
        }
    }
}
