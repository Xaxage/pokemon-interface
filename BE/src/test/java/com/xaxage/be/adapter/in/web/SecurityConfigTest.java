package com.xaxage.be.adapter.in.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtValidationException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Checking bearer tokens")
class SecurityConfigTest {

    private static final String SECRET = "unit-test-secret-that-is-at-least-256-bits-long";

    @Nested
    @DisplayName("Must accept only valid, unexpired tokens on protected actions")
    class AcceptsOnlyUnexpiredTokens {

        @Test
        @DisplayName("The one where the token expired a second ago, so it is no longer accepted")
        void tokenThatJustExpiredIsRejected() {
            var now = Instant.now().getEpochSecond();
            var decoder = new SecurityConfig().jwtDecoder(SECRET);

            assertThatThrownBy(() -> decoder.decode(signedToken(now - 3601, now - 1)))
                    .isInstanceOf(JwtValidationException.class);
        }
    }

    private static String signedToken(long issuedAt, long expiresAt) {
        var encoder = Base64.getUrlEncoder().withoutPadding();
        var header = encoder.encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        var claims = encoder.encodeToString("{\"sub\":\"ash\",\"iat\":%d,\"exp\":%d}"
                .formatted(issuedAt, expiresAt).getBytes(StandardCharsets.UTF_8));
        try {
            var hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            var signature = encoder.encodeToString(
                    hmac.doFinal((header + "." + claims).getBytes(StandardCharsets.UTF_8)));
            return header + "." + claims + "." + signature;
        } catch (GeneralSecurityException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
