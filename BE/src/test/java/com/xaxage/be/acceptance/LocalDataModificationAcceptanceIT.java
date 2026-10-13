package com.xaxage.be.acceptance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * US-04 Local Data Modification — see {@code doc/specs/local-data-modification.md}.
 *
 * <p>Only the local database is involved, so PokeAPI is pointed at an address nothing listens on.
 * The local database starts every test empty.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("US-04 Local Data Modification")
class LocalDataModificationAcceptanceIT {

    /**
     * The key the application checks token signatures with, set for this test run only.
     */
    private static final String TOKEN_SECRET = "acceptance-test-secret-that-is-at-least-256-bits-long";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcClient database;

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("pokeapi.base-url", () -> "http://localhost:1/pokeapi-is-not-used");
        registry.add("security.jwt.secret", () -> TOKEN_SECRET);
    }

    @BeforeEach
    void startWithAnEmptyLocalDatabase() {
        database.sql("DELETE FROM stored_pokemon_tag").update();
        database.sql("DELETE FROM stored_pokemon").update();
    }

    @Nested
    @DisplayName("Must replace our own fields of a stored Pokémon with the ones sent")
    class ReplacesOurOwnFieldsWithTheOnesSent {

        @Test
        @DisplayName("The one where Bulbasaur is updated with localized name \"Bisasam\", region \"Kanto\" and tags "
                + "[\"starter\", \"grass\"], and comes back with those values (200)")
        void bulbasaurComesBackWithTheValuesSent() {
            storesBulbasaur();

            var response = mvc.put().uri("/api/stored-pokemon/{id}", 1)
                    .header(HttpHeaders.AUTHORIZATION, signedIn())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"localizedName":"Bisasam","region":"Kanto","tags":["starter","grass"]}""")
                    .exchange();

            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().extractingPath("$.id").isEqualTo(1);
            assertThat(response).bodyJson().extractingPath("$.name").isEqualTo("bulbasaur");
            assertThat(response).bodyJson().extractingPath("$.localizedName").isEqualTo("Bisasam");
            assertThat(response).bodyJson().extractingPath("$.region").isEqualTo("Kanto");
            assertThat(response).bodyJson().extractingPath("$.tags").asArray().containsExactly("starter", "grass");
        }

        @Test
        @DisplayName("The one where a field is left out of the update, so it is cleared (a full replace, not a merge)")
        void fieldLeftOutIsCleared() {
            storesBulbasaur();
            database.sql("UPDATE stored_pokemon SET localized_name = 'Bisasam', region = 'Kanto' WHERE id = 1")
                    .update();

            var response = mvc.put().uri("/api/stored-pokemon/{id}", 1)
                    .header(HttpHeaders.AUTHORIZATION, signedIn())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"localizedName":"Bisasam","tags":["starter"]}""")
                    .exchange();

            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().extractingPath("$.localizedName").isEqualTo("Bisasam");
            assertThat(response).bodyJson().hasPath("$.region");
            assertThat(response).bodyJson().extractingPath("$.region").isNull();
            var viewed = mvc.get().uri("/api/stored-pokemon/{id}", 1).exchange();
            assertThat(viewed).bodyJson().hasPath("$.region");
            assertThat(viewed).bodyJson().extractingPath("$.region").isNull();
        }

        @Test
        @DisplayName("The one where the update also sends a different weight, so the weight is ignored and stays 69")
        void weightSentIsIgnored() {
            storesBulbasaur();

            var response = mvc.put().uri("/api/stored-pokemon/{id}", 1)
                    .header(HttpHeaders.AUTHORIZATION, signedIn())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"localizedName":"Bisasam","weight":1000}""")
                    .exchange();

            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().extractingPath("$.weight").isEqualTo(69);
            var viewed = mvc.get().uri("/api/stored-pokemon/{id}", 1).exchange();
            assertThat(viewed).bodyJson().extractingPath("$.weight").isEqualTo(69);
        }
    }

    /**
     * @return the Authorization header of a user who has just signed in: a bearer token signed with HS256, valid
     * for the next hour
     */
    private static String signedIn() {
        var now = Instant.now().getEpochSecond();
        return "Bearer " + token(now, now + 3600);
    }

    /**
     * @return a JWT signed with HS256 using the application's key, issued and expiring at the given epoch seconds
     */
    private static String token(long issuedAt, long expiresAt) {
        var encoder = Base64.getUrlEncoder().withoutPadding();
        var header = encoder.encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        var claims = encoder.encodeToString("{\"sub\":\"demo\",\"iat\":%d,\"exp\":%d}"
                .formatted(issuedAt, expiresAt).getBytes(StandardCharsets.UTF_8));
        try {
            var hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(TOKEN_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            var signature = encoder.encodeToString(
                    hmac.doFinal((header + "." + claims).getBytes(StandardCharsets.UTF_8)));
            return header + "." + claims + "." + signature;
        } catch (GeneralSecurityException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    /**
     * Stores Bulbasaur as synchronisation leaves it: PokeAPI's fields, and none of our own.
     */
    private void storesBulbasaur() {
        database.sql("""
                        INSERT INTO stored_pokemon (id, name, category, weight, sprite, localized_name, region)
                        VALUES (1, 'bulbasaur', 'Seed Pokémon', 69, 'bulbasaur.png', NULL, NULL)""")
                .update();
    }
}
