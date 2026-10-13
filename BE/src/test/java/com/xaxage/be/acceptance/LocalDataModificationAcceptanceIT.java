package com.xaxage.be.acceptance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
import java.util.stream.IntStream;
import java.util.stream.Stream;

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

    @Nested
    @DisplayName("Must reject updates with invalid values (400, with the standard error body listing each invalid field)")
    class RejectsUpdatesWithInvalidValues {

        static Stream<Arguments> rejectedValues() {
            return Stream.of(
                    Arguments.of("a blank localized name", """
                            {"localizedName":"  "}""", "localizedName"),
                    Arguments.of("a localized name over 50 characters", """
                            {"localizedName":"%s"}""".formatted("a".repeat(51)), "localizedName"),
                    Arguments.of("a blank region", """
                            {"region":""}""", "region"),
                    Arguments.of("a region over 50 characters", """
                            {"region":"%s"}""".formatted("k".repeat(51)), "region"),
                    Arguments.of("more than 10 tags", """
                            {"tags":["t1","t2","t3","t4","t5","t6","t7","t8","t9","t10","t11"]}""", "tags"),
                    Arguments.of("a blank tag", """
                            {"tags":["starter"," "]}""", "tags[1]"),
                    Arguments.of("a tag over 30 characters", """
                            {"tags":["%s"]}""".formatted("t".repeat(31)), "tags[0]"),
                    Arguments.of("a duplicate tag", """
                            {"tags":["starter","grass","starter"]}""", "tags"));
        }

        @ParameterizedTest(name = "The one where the update sends {0}, so it is rejected and nothing is changed")
        @MethodSource("rejectedValues")
        @DisplayName("The one where the update sends a value the table rejects, so the reader gets a 400 naming the "
                + "field and nothing is changed")
        void invalidValueIsRejectedAndNothingIsChanged(String what, String body, String invalidField) {
            storesBulbasaurAsBisasamFromKanto();

            var response = mvc.put().uri("/api/stored-pokemon/{id}", 1)
                    .header(HttpHeaders.AUTHORIZATION, signedIn())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
                    .exchange();

            assertThat(response).hasStatus(HttpStatus.BAD_REQUEST);
            assertThat(response).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
            assertThat(response).bodyJson().extractingPath("$.status").isEqualTo(400);
            assertThat(response).bodyJson().extractingPath("$.invalidFields[*].field").asArray()
                    .containsExactly(invalidField);
            assertBulbasaurIsStillBisasamFromKanto();
        }

        @Test
        @DisplayName("The one where both the localized name and the region are blank, so the reader gets a 400 listing "
                + "both fields")
        void eachInvalidFieldIsListed() {
            storesBulbasaurAsBisasamFromKanto();

            var response = mvc.put().uri("/api/stored-pokemon/{id}", 1)
                    .header(HttpHeaders.AUTHORIZATION, signedIn())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"localizedName":"","region":" "}""")
                    .exchange();

            assertThat(response).hasStatus(HttpStatus.BAD_REQUEST);
            assertThat(response).bodyJson().extractingPath("$.invalidFields[*].field").asArray()
                    .containsExactlyInAnyOrder("localizedName", "region");
            assertBulbasaurIsStillBisasamFromKanto();
        }

        @Test
        @DisplayName("The one where the localized name and region are 50 characters and there are 10 tags of 30 "
                + "characters, so the update is accepted (200)")
        void valuesAtTheLimitsAreAccepted() {
            storesBulbasaur();
            var name = "n".repeat(50);
            var region = "r".repeat(50);
            var tags = IntStream.rangeClosed(0, 9).mapToObj(i -> i + "t".repeat(29)).toList();

            var response = mvc.put().uri("/api/stored-pokemon/{id}", 1)
                    .header(HttpHeaders.AUTHORIZATION, signedIn())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"localizedName":"%s","region":"%s","tags":["%s"]}"""
                            .formatted(name, region, String.join("\",\"", tags)))
                    .exchange();

            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().extractingPath("$.localizedName").isEqualTo(name);
            assertThat(response).bodyJson().extractingPath("$.region").isEqualTo(region);
            assertThat(response).bodyJson().extractingPath("$.tags").asArray().hasSize(10);
        }

        @Test
        @DisplayName("The one where the body is not valid JSON, so the reader gets a 400 and nothing is changed")
        void bodyThatIsNotValidJsonIsRejected() {
            storesBulbasaurAsBisasamFromKanto();

            var response = mvc.put().uri("/api/stored-pokemon/{id}", 1)
                    .header(HttpHeaders.AUTHORIZATION, signedIn())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"localizedName\": \"Bulby\"")
                    .exchange();

            assertThat(response).hasStatus(HttpStatus.BAD_REQUEST);
            assertThat(response).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
            assertThat(response).bodyJson().extractingPath("$.status").isEqualTo(400);
            assertBulbasaurIsStillBisasamFromKanto();
        }

        private void storesBulbasaurAsBisasamFromKanto() {
            storesBulbasaur();
            database.sql("UPDATE stored_pokemon SET localized_name = 'Bisasam', region = 'Kanto' WHERE id = 1")
                    .update();
        }

        private void assertBulbasaurIsStillBisasamFromKanto() {
            var viewed = mvc.get().uri("/api/stored-pokemon/{id}", 1).exchange();
            assertThat(viewed).bodyJson().extractingPath("$.localizedName").isEqualTo("Bisasam");
            assertThat(viewed).bodyJson().extractingPath("$.region").isEqualTo("Kanto");
            assertThat(viewed).bodyJson().extractingPath("$.tags").asArray().isEmpty();
        }
    }

    @Nested
    @DisplayName("Must delete a stored Pokémon")
    class DeletesAStoredPokemon {

        @Test
        @DisplayName("The one where Bulbasaur is deleted (204), and viewing it afterwards gives a 404")
        void deletedBulbasaurIsNoLongerStored() {
            storesBulbasaur();
            database.sql("INSERT INTO stored_pokemon_tag (pokemon_id, position, tag) VALUES (1, 0, 'starter')")
                    .update();

            var response = mvc.delete().uri("/api/stored-pokemon/{id}", 1)
                    .header(HttpHeaders.AUTHORIZATION, signedIn())
                    .exchange();

            assertThat(response).hasStatus(HttpStatus.NO_CONTENT);
            var viewed = mvc.get().uri("/api/stored-pokemon/{id}", 1).exchange();
            assertThat(viewed).hasStatus(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("Must answer 404 when the Pokémon is not stored locally")
    class Answers404WhenThePokemonIsNotStoredLocally {

        @Test
        @DisplayName("The one where id 99999 is updated but not stored, so the reader gets a 404 and nothing is stored")
        void updatingAPokemonThatIsNotStoredIsNotFound() {
            var response = mvc.put().uri("/api/stored-pokemon/{id}", 99999)
                    .header(HttpHeaders.AUTHORIZATION, signedIn())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"localizedName":"Bisasam"}""")
                    .exchange();

            assertThat(response).hasStatus(HttpStatus.NOT_FOUND);
            assertThat(response).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
            assertThat(response).bodyJson().extractingPath("$.status").isEqualTo(404);
            assertThat(response).bodyJson().extractingPath("$.title").isEqualTo("Not Found");
            assertThat(database.sql("SELECT COUNT(*) FROM stored_pokemon").query(Integer.class).single())
                    .isZero();
        }

        @Test
        @DisplayName("The one where id 99999 is deleted but not stored, so the reader gets a 404")
        void deletingAPokemonThatIsNotStoredIsNotFound() {
            var response = mvc.delete().uri("/api/stored-pokemon/{id}", 99999)
                    .header(HttpHeaders.AUTHORIZATION, signedIn())
                    .exchange();

            assertThat(response).hasStatus(HttpStatus.NOT_FOUND);
            assertThat(response).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
            assertThat(response).bodyJson().extractingPath("$.status").isEqualTo(404);
            assertThat(response).bodyJson().extractingPath("$.title").isEqualTo("Not Found");
        }
    }

    @Nested
    @DisplayName("Must let only signed-in users update or delete")
    class LetsOnlySignedInUsersUpdateOrDelete {

        @Test
        @DisplayName("The one where an update with no token gets a 401 and nothing is changed")
        void updateWithNoTokenIsUnauthorizedAndChangesNothing() {
            storesBulbasaur();

            var response = mvc.put().uri("/api/stored-pokemon/{id}", 1)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"localizedName":"Bisasam","region":"Kanto","tags":["starter"]}""")
                    .exchange();

            assertThat(response).hasStatus(HttpStatus.UNAUTHORIZED);
            assertThat(response).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
            assertThat(response).bodyJson().extractingPath("$.status").isEqualTo(401);
            assertBulbasaurIsUnchanged();
        }

        @Test
        @DisplayName("The one where the token has expired, so the update gets a 401 as well")
        void updateWithAnExpiredTokenIsUnauthorized() {
            storesBulbasaur();
            var now = Instant.now().getEpochSecond();

            var response = mvc.put().uri("/api/stored-pokemon/{id}", 1)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(now - 7200, now - 3600))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"localizedName":"Bisasam","region":"Kanto","tags":["starter"]}""")
                    .exchange();

            assertThat(response).hasStatus(HttpStatus.UNAUTHORIZED);
            assertThat(response).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
            assertThat(response).bodyJson().extractingPath("$.status").isEqualTo(401);
            assertBulbasaurIsUnchanged();
        }

        private void assertBulbasaurIsUnchanged() {
            var viewed = mvc.get().uri("/api/stored-pokemon/{id}", 1).exchange();
            assertThat(viewed).bodyJson().hasPath("$.localizedName");
            assertThat(viewed).bodyJson().extractingPath("$.localizedName").isNull();
            assertThat(viewed).bodyJson().hasPath("$.region");
            assertThat(viewed).bodyJson().extractingPath("$.region").isNull();
            assertThat(viewed).bodyJson().extractingPath("$.tags").asArray().isEmpty();
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
