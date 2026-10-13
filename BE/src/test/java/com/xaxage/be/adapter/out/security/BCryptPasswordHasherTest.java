package com.xaxage.be.adapter.out.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Password hashing with BCrypt")
class BCryptPasswordHasherTest {

    @Nested
    @DisplayName("Must never store passwords in readable form")
    class NeverStoresPasswordsInReadableForm {

        @Test
        @DisplayName("The one where \"pikachu123\" is hashed, so the hash is a BCrypt hash that does not contain it")
        void hashIsBCryptAndDoesNotContainThePassword() {
            var hash = new BCryptPasswordHasher().hash("pikachu123");

            assertThat(hash).doesNotContain("pikachu123");
            assertThat(hash).matches("\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}");
        }
    }

    @Nested
    @DisplayName("Must give a token only to a user who signs in with the right credentials")
    class TellsTheRightPasswordFromAWrongOne {

        @Test
        @DisplayName("The one where the hash of \"pikachu123\" matches \"pikachu123\" but not \"raichu1234\"")
        void onlyTheRightPasswordMatchesItsHash() {
            var hasher = new BCryptPasswordHasher();
            var hash = hasher.hash("pikachu123");

            assertThat(hasher.matches("pikachu123", hash)).isTrue();
            assertThat(hasher.matches("raichu1234", hash)).isFalse();
        }
    }
}
