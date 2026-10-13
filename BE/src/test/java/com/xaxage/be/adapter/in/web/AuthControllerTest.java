package com.xaxage.be.adapter.in.web;

import com.xaxage.be.application.port.in.RegisterUserUseCase;
import com.xaxage.be.domain.exception.UsernameTakenException;
import com.xaxage.be.domain.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@DisplayName("Authentication endpoints")
class AuthControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private RegisterUserUseCase registerUser;

    @Nested
    @DisplayName("Must register a new user with a username and password")
    class RegistersANewUser {

        @Test
        @DisplayName("The one where \"ash\" registers with password \"pikachu123\" and gets 201, without the password "
                + "in the response")
        void registrationAnswers201WithoutThePassword() {
            given(registerUser.register("ash", "pikachu123")).willReturn(new User("ash", "pikachu123"));

            var response = mvc.post().uri("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"username":"ash","password":"pikachu123"}""")
                    .exchange();

            assertThat(response).hasStatus(HttpStatus.CREATED);
            assertThat(response).bodyJson().extractingPath("$.username").isEqualTo("ash");
            assertThat(response).bodyText().doesNotContain("pikachu123");
        }

        @Test
        @DisplayName("The one where \"ash\" is already taken, so the reader gets a 409 with the standard error body")
        void takenUsernameAnswers409() {
            given(registerUser.register("ash", "pikachu123")).willThrow(new UsernameTakenException("ash"));

            var response = mvc.post().uri("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"username":"ash","password":"pikachu123"}""")
                    .exchange();

            assertThat(response).hasStatus(HttpStatus.CONFLICT);
            assertThat(response).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
            assertThat(response).bodyJson().extractingPath("$.status").isEqualTo(409);
            assertThat(response).bodyJson().extractingPath("$.detail").isEqualTo("The username ash is already taken");
        }
    }

    @Nested
    @DisplayName("Must reject registrations with invalid values (400, with the standard error body)")
    class RejectsRegistrationsWithInvalidValues {

        @ParameterizedTest(name = "The one where \"{0}\" registers with \"{1}\", so it is rejected naming the {2}")
        @CsvSource({
                "as, pikachu123, username",
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa, pikachu123, username",
                "ash, short, password"})
        @DisplayName("The one where the username or password breaks the limits, so the reader gets a 400 naming the "
                + "field and no one is registered")
        void invalidRegistrationAnswers400NamingTheField(String username, String password, String invalidField) {
            var response = mvc.post().uri("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"username":"%s","password":"%s"}""".formatted(username, password))
                    .exchange();

            assertThat(response).hasStatus(HttpStatus.BAD_REQUEST);
            assertThat(response).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
            assertThat(response).bodyJson().extractingPath("$.invalidFields[*].field").asArray()
                    .containsExactly(invalidField);
            then(registerUser).shouldHaveNoInteractions();
        }

        @ParameterizedTest(name = "The one where the {1} is left out, so it is rejected naming the {1}")
        @CsvSource(delimiter = '|', value = {
                "{\"password\":\"pikachu123\"} | username",
                "{\"username\":\"ash\"}        | password"})
        @DisplayName("The one where the username or password is left out, so the reader gets a 400 naming the field "
                + "and no one is registered")
        void missingValueAnswers400NamingTheField(String body, String invalidField) {
            var response = mvc.post().uri("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
                    .exchange();

            assertThat(response).hasStatus(HttpStatus.BAD_REQUEST);
            assertThat(response).bodyJson().extractingPath("$.invalidFields[*].field").asArray()
                    .containsExactly(invalidField);
            then(registerUser).shouldHaveNoInteractions();
        }
    }
}
