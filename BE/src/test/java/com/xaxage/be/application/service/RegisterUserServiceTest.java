package com.xaxage.be.application.service;

import com.xaxage.be.application.port.out.UserRepositoryPort;
import com.xaxage.be.domain.exception.UsernameTakenException;
import com.xaxage.be.domain.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@DisplayName("Registering a new user")
class RegisterUserServiceTest {

    @Nested
    @DisplayName("Must register a new user with a username and password")
    class RegistersANewUser {

        @Test
        @DisplayName("The one where \"ash\" registers with password \"pikachu123\", so ash is stored and returned")
        void ashIsStoredAndReturned() {
            var users = mock(UserRepositoryPort.class);
            given(users.save(any())).willAnswer(invocation -> invocation.getArgument(0));

            var registered = new RegisterUserService(users).register("ash", "pikachu123");

            assertThat(registered.username()).isEqualTo("ash");
            then(users).should().save(new User("ash", "pikachu123"));
        }

        @Test
        @DisplayName("The one where \"ash\" is already taken, so it is reported as taken and nothing is stored")
        void takenUsernameIsReportedAndNothingIsStored() {
            var users = mock(UserRepositoryPort.class);
            given(users.existsByUsername("ash")).willReturn(true);

            var service = new RegisterUserService(users);

            assertThatThrownBy(() -> service.register("ash", "another-password"))
                    .isInstanceOf(UsernameTakenException.class)
                    .hasMessage("The username ash is already taken");
            then(users).should(never()).save(any());
        }
    }
}
