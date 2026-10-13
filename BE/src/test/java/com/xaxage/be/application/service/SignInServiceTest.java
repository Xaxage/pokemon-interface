package com.xaxage.be.application.service;

import com.xaxage.be.application.port.out.PasswordHasherPort;
import com.xaxage.be.application.port.out.TokenIssuerPort;
import com.xaxage.be.application.port.out.UserRepositoryPort;
import com.xaxage.be.domain.exception.InvalidCredentialsException;
import com.xaxage.be.domain.model.AccessToken;
import com.xaxage.be.domain.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@DisplayName("Signing in")
class SignInServiceTest {

    @Nested
    @DisplayName("Must give a token valid for 1 hour to a user who signs in with the right credentials")
    class GivesATokenValidForOneHour {

        @Test
        @DisplayName("The one where \"ash\" signs in with \"pikachu123\", so ash gets a token valid for 1 hour")
        void ashGetsATokenValidForOneHour() {
            var users = mock(UserRepositoryPort.class);
            given(users.findByUsername("ash")).willReturn(Optional.of(new User("ash", "hash-of-pikachu123")));
            var passwords = mock(PasswordHasherPort.class);
            given(passwords.matches("pikachu123", "hash-of-pikachu123")).willReturn(true);
            var tokens = mock(TokenIssuerPort.class);
            given(tokens.issue("ash", Duration.ofHours(1))).willReturn("token-for-ash");

            var token = new SignInService(users, passwords, tokens).signIn("ash", "pikachu123");

            assertThat(token).isEqualTo(new AccessToken("token-for-ash"));
        }

        @Test
        @DisplayName("The one where the password is wrong, so the credentials are rejected and no token is issued")
        void wrongPasswordIsRejected() {
            var users = mock(UserRepositoryPort.class);
            given(users.findByUsername("ash")).willReturn(Optional.of(new User("ash", "hash-of-pikachu123")));
            var passwords = mock(PasswordHasherPort.class);
            given(passwords.matches("raichu1234", "hash-of-pikachu123")).willReturn(false);
            var tokens = mock(TokenIssuerPort.class);

            var service = new SignInService(users, passwords, tokens);

            assertThatThrownBy(() -> service.signIn("ash", "raichu1234"))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessage("The username or password is wrong");
            then(tokens).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("The one where the user does not exist, so the credentials are rejected just as for a wrong "
                + "password and no token is issued")
        void unknownUserIsRejectedLikeAWrongPassword() {
            var users = mock(UserRepositoryPort.class);
            given(users.findByUsername("misty")).willReturn(Optional.empty());
            var tokens = mock(TokenIssuerPort.class);

            var service = new SignInService(users, mock(PasswordHasherPort.class), tokens);

            assertThatThrownBy(() -> service.signIn("misty", "pikachu123"))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessage("The username or password is wrong");
            then(tokens).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("The one where the user does not exist, so the password is still checked against a hash, taking "
                + "as long as a wrong password and giving away nothing by its timing")
        void unknownUserTakesAsLongAsAWrongPassword() {
            var users = mock(UserRepositoryPort.class);
            given(users.findByUsername("misty")).willReturn(Optional.empty());
            var passwords = mock(PasswordHasherPort.class);
            given(passwords.hash(anyString())).willReturn("hash-of-nobody");

            var service = new SignInService(users, passwords, mock(TokenIssuerPort.class));

            assertThatThrownBy(() -> service.signIn("misty", "pikachu123"))
                    .isInstanceOf(InvalidCredentialsException.class);
            then(passwords).should().matches("pikachu123", "hash-of-nobody");
        }
    }
}
