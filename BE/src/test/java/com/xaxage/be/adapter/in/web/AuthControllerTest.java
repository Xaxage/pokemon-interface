package com.xaxage.be.adapter.in.web;

import com.xaxage.be.application.port.in.RegisterUserUseCase;
import com.xaxage.be.domain.exception.UsernameTakenException;
import com.xaxage.be.domain.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

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
}
