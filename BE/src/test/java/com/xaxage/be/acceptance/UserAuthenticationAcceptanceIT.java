package com.xaxage.be.acceptance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

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

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcClient database;

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("pokeapi.base-url", () -> "http://localhost:1/pokeapi-is-not-used");
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

    private MvcTestResult register(String username, String password) {
        return mvc.post().uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"%s","password":"%s"}""".formatted(username, password))
                .exchange();
    }
}
