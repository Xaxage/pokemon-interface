package com.xaxage.be.application.service;

import com.xaxage.be.application.port.out.PasswordHasherPort;
import com.xaxage.be.application.port.out.UserRepositoryPort;
import com.xaxage.be.domain.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@DisplayName("Providing the demo user")
class ProvideDemoUserServiceTest {

    @Nested
    @DisplayName("Must come with a demo user")
    class ComesWithADemoUser {

        @Test
        @DisplayName("The one where no demo user is registered, so \"demo\" is registered with the hash of "
                + "\"demo1234\"")
        void registersTheDemoUserWhenMissing() {
            var users = mock(UserRepositoryPort.class);
            given(users.existsByUsername("demo")).willReturn(false);
            var passwords = mock(PasswordHasherPort.class);
            given(passwords.hash("demo1234")).willReturn("hash-of-demo1234");

            new ProvideDemoUserService(users, passwords).provide();

            then(users).should().save(new User("demo", "hash-of-demo1234"));
        }

        @Test
        @DisplayName("The one where the demo user is already registered, so nothing is registered and nothing is "
                + "overwritten")
        void leavesAnExistingDemoUserAlone() {
            var users = mock(UserRepositoryPort.class);
            given(users.existsByUsername("demo")).willReturn(true);

            new ProvideDemoUserService(users, mock(PasswordHasherPort.class)).provide();

            then(users).should(never()).save(any());
        }
    }
}
