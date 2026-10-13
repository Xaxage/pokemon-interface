package com.xaxage.be.acceptance;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * US-05 User Authentication — see {@code doc/specs/user-authentication.md}.
 *
 * <p>PokeAPI is not involved, so it is pointed at an address nothing listens on.
 * Every test starts with no registered users apart from the demo user.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("US-05 User Authentication")
class UserAuthenticationAcceptanceIT {

    /**
     * The key the application signs and checks tokens with, set for this test run only.
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
    void startWithOnlyTheDemoUser() {
        database.sql("DELETE FROM app_user WHERE username <> 'demo'").update();
    }

    @Nested
    @DisplayName("Must register a new user with a username and password")
    class RegistersANewUser {

        @Test
        @DisplayName("The one where \"ash\" registers with password \"pikachu123\" and gets 201, without the password "
                + "in the response")
        void ashIsRegisteredWithoutThePasswordInTheResponse() {
            var response = register("ash", "pikachu123");

            assertThat(response).hasStatus(HttpStatus.CREATED);
            assertThat(response).bodyJson().extractingPath("$.username").isEqualTo("ash");
            assertThat(response).bodyJson().doesNotHavePath("$.password");
            assertThat(response).bodyText().doesNotContain("pikachu123");
        }

        @Test
        @DisplayName("The one where \"ash\" is already taken, so the reader gets a 409 with the standard error body")
        void takenUsernameIsAConflict() {
            register("ash", "pikachu123");

            var response = register("ash", "another-password");

            assertThat(response).hasStatus(HttpStatus.CONFLICT);
            assertThat(response).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
            assertThat(response).bodyJson().extractingPath("$.status").isEqualTo(409);
            assertThat(response).bodyJson().extractingPath("$.title").isEqualTo("Conflict");
        }
    }

    @Nested
    @DisplayName("Must reject registrations with invalid values (400, with the standard error body)")
    class RejectsRegistrationsWithInvalidValues {

        @Test
        @DisplayName("The one where \"ash\" registers with \"pikachu123\", so the registration is accepted")
        void validRegistrationIsAccepted() {
            var response = register("ash", "pikachu123");

            assertThat(response).hasStatus(HttpStatus.CREATED);
        }

        @ParameterizedTest(name = "The one where \"{0}\" registers with \"{1}\", so it is rejected ({2})")
        @CsvSource({
                "as, pikachu123, username under 3 characters, username",
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa, pikachu123, username over 30 characters, username",
                "ash, short, password under 8 characters, password"})
        @DisplayName("The one where the username or password breaks the limits, so the reader gets a 400 naming the "
                + "field and no one is registered")
        void invalidRegistrationIsRejected(String username, String password, String why, String invalidField) {
            var response = register(username, password);

            assertThat(response).hasStatus(HttpStatus.BAD_REQUEST);
            assertThat(response).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
            assertThat(response).bodyJson().extractingPath("$.status").isEqualTo(400);
            assertThat(response).bodyJson().extractingPath("$.invalidFields[*].field").asArray()
                    .containsExactly(invalidField);
            assertThat(database.sql("SELECT COUNT(*) FROM app_user WHERE username = ?").param(username)
                    .query(Integer.class).single()).isZero();
        }
    }

    @Nested
    @DisplayName("Must never store passwords in readable form")
    class NeverStoresPasswordsInReadableForm {

        @Test
        @DisplayName("The one where the stored password for \"ash\" is a hash, not \"pikachu123\"")
        void storedPasswordIsAHash() {
            register("ash", "pikachu123");

            var stored = database.sql("SELECT password_hash FROM app_user WHERE username = 'ash'")
                    .query(String.class).single();

            assertThat(stored).doesNotContain("pikachu123");
            assertThat(stored).matches("\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}");
        }
    }

    @Nested
    @DisplayName("Must give a token valid for 1 hour to a user who signs in with the right credentials")
    class GivesATokenValidForOneHour {

        @Test
        @DisplayName("The one where \"ash\" signs in with \"pikachu123\" and gets a token (200)")
        void ashGetsATokenValidForOneHour() throws UnsupportedEncodingException {
            register("ash", "pikachu123");

            var response = signIn("ash", "pikachu123");

            assertThat(response).hasStatusOk();
            var claims = claimsOf(JsonPath.read(response.getResponse().getContentAsString(), "$.token"));
            assertThat((String) JsonPath.read(claims, "$.sub")).isEqualTo("ash");
            long issuedAt = ((Number) JsonPath.read(claims, "$.iat")).longValue();
            long expiresAt = ((Number) JsonPath.read(claims, "$.exp")).longValue();
            assertThat(expiresAt - issuedAt).isEqualTo(3600);
        }

        @Test
        @DisplayName("The one where the password is wrong or the user does not exist; both get the same 401, so the "
                + "reader cannot tell which")
        void wrongPasswordAndUnknownUserGetTheSame401() throws UnsupportedEncodingException {
            register("ash", "pikachu123");

            var wrongPassword = signIn("ash", "raichu1234");
            var unknownUser = signIn("misty", "pikachu123");

            assertThat(wrongPassword).hasStatus(HttpStatus.UNAUTHORIZED);
            assertThat(wrongPassword).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
            assertThat(wrongPassword).bodyJson().extractingPath("$.status").isEqualTo(401);
            assertThat(unknownUser).hasStatus(HttpStatus.UNAUTHORIZED);
            assertThat(unknownUser.getResponse().getContentAsString())
                    .isEqualTo(wrongPassword.getResponse().getContentAsString());
        }
    }

    @Nested
    @DisplayName("Must accept only valid, unexpired tokens on protected actions")
    class AcceptsOnlyValidUnexpiredTokens {

        @BeforeEach
        void bulbasaurIsStored() {
            database.sql("DELETE FROM stored_pokemon_tag WHERE pokemon_id = 1").update();
            database.sql("DELETE FROM stored_pokemon WHERE id = 1").update();
            database.sql("""
                            INSERT INTO stored_pokemon (id, name, category, weight, sprite, localized_name, region)
                            VALUES (1, 'bulbasaur', 'Seed Pokémon', 69, 'bulbasaur.png', NULL, NULL)""")
                    .update();
        }

        @Test
        @DisplayName("The one where no token is sent, so the protected action gets a 401")
        void noTokenIsUnauthorized() {
            var response = mvc.delete().uri("/api/stored-pokemon/{id}", 1).exchange();

            assertIsUnauthorizedAndBulbasaurIsStillStored(response);
        }

        @Test
        @DisplayName("The one where the token is tampered with, so the protected action gets a 401")
        void tamperedTokenIsUnauthorized() throws UnsupportedEncodingException {
            var parts = tokenFor("ash", "pikachu123").split("\\.");
            var tamperedClaims = Base64.getUrlEncoder().withoutPadding().encodeToString(
                    claimsOf(String.join(".", parts)).replace("\"ash\"", "\"gary\"").getBytes(StandardCharsets.UTF_8));

            var response = mvc.delete().uri("/api/stored-pokemon/{id}", 1)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + parts[0] + "." + tamperedClaims + "." + parts[2])
                    .exchange();

            assertIsUnauthorizedAndBulbasaurIsStillStored(response);
        }

        @Test
        @DisplayName("The one where the token is malformed, so the protected action gets a 401")
        void malformedTokenIsUnauthorized() {
            var response = mvc.delete().uri("/api/stored-pokemon/{id}", 1)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-token")
                    .exchange();

            assertIsUnauthorizedAndBulbasaurIsStillStored(response);
        }

        @Test
        @DisplayName("The one where the token is over 1 hour old, so it has expired and the protected action gets a "
                + "401")
        void expiredTokenIsUnauthorized() {
            var now = Instant.now().getEpochSecond();

            var response = mvc.delete().uri("/api/stored-pokemon/{id}", 1)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + signedToken("ash", now - 3601, now - 1))
                    .exchange();

            assertIsUnauthorizedAndBulbasaurIsStillStored(response);
        }

        @Test
        @DisplayName("The one where the token is valid, so the protected action is allowed")
        void validTokenIsAllowed() throws UnsupportedEncodingException {
            var token = tokenFor("ash", "pikachu123");

            var response = mvc.delete().uri("/api/stored-pokemon/{id}", 1)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .exchange();

            assertThat(response).hasStatus(HttpStatus.NO_CONTENT);
            assertThat(database.sql("SELECT COUNT(*) FROM stored_pokemon WHERE id = 1").query(Integer.class).single())
                    .isZero();
        }

        private void assertIsUnauthorizedAndBulbasaurIsStillStored(MvcTestResult response) {
            assertThat(response).hasStatus(HttpStatus.UNAUTHORIZED);
            assertThat(response).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
            assertThat(response).bodyJson().extractingPath("$.status").isEqualTo(401);
            assertThat(database.sql("SELECT COUNT(*) FROM stored_pokemon WHERE id = 1").query(Integer.class).single())
                    .isOne();
        }

        /**
         * @return the token the user gets on registering and signing in
         */
        private String tokenFor(String username, String password) throws UnsupportedEncodingException {
            register(username, password);
            return JsonPath.read(signIn(username, password).getResponse().getContentAsString(), "$.token");
        }
    }

    @Nested
    @DisplayName("Must come with a demo user")
    class ComesWithADemoUser {

        @Test
        @DisplayName("The one where the app starts and \"demo\" / \"demo1234\" can sign in straight away")
        void demoCanSignInStraightAway() {
            var response = signIn("demo", "demo1234");

            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().extractingPath("$.token").isNotNull();
        }
    }

    /**
     * @return a JWT signed with HS256 using the application's key, issued and expiring at the given epoch seconds
     */
    private static String signedToken(String subject, long issuedAt, long expiresAt) {
        var encoder = Base64.getUrlEncoder().withoutPadding();
        var header = encoder.encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        var claims = encoder.encodeToString("{\"sub\":\"%s\",\"iat\":%d,\"exp\":%d}"
                .formatted(subject, issuedAt, expiresAt).getBytes(StandardCharsets.UTF_8));
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

    private MvcTestResult signIn(String username, String password) {
        return mvc.post().uri("/api/auth/sign-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"%s","password":"%s"}""".formatted(username, password))
                .exchange();
    }

    /**
     * @return the claims of the JWT, as JSON
     */
    private static String claimsOf(String token) {
        return new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8);
    }

    private MvcTestResult register(String username, String password) {
        return mvc.post().uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"%s","password":"%s"}""".formatted(username, password))
                .exchange();
    }
}
