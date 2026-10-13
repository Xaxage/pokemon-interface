package com.xaxage.be.adapter.out.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Issuing signed tokens")
class JwtTokenIssuerTest {

    @Nested
    @DisplayName("Must give a token valid for 1 hour to a user who signs in with the right credentials")
    class GivesATokenValidForOneHour {

        @Test
        @DisplayName("The one where a token is issued to ash for 1 hour, so it is signed with the secret, names ash "
                + "and expires 1 hour after it was issued")
        void tokenNamesAshAndExpiresAfterOneHour() {
            var secret = "unit-test-secret-that-is-at-least-256-bits-long";
            var now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
            var issuer = new JwtTokenIssuer(secret, Clock.fixed(now, ZoneOffset.UTC));

            var token = issuer.issue("ash", Duration.ofHours(1));

            var decoded = NimbusJwtDecoder
                    .withSecretKey(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
                    .build()
                    .decode(token);
            assertThat(decoded.getSubject()).isEqualTo("ash");
            assertThat(decoded.getIssuedAt()).isEqualTo(now);
            assertThat(decoded.getExpiresAt()).isEqualTo(now.plusSeconds(3600));
        }
    }
}
