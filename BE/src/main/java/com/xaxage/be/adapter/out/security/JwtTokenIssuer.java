package com.xaxage.be.adapter.out.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.xaxage.be.application.port.out.TokenIssuerPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;

/**
 * Issues JWTs signed with HS256 using the configured secret, the same one incoming tokens are checked with.
 */
@Component
class JwtTokenIssuer implements TokenIssuerPort {

    private final JwtEncoder encoder;
    private final Clock clock;

    @Autowired
    JwtTokenIssuer(@Value("${security.jwt.secret}") String secret) {
        this(secret, Clock.systemUTC());
    }

    JwtTokenIssuer(String secret, Clock clock) {
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(
                new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256")));
        this.clock = clock;
    }

    @Override
    public String issue(String username, Duration lifetime) {
        var issuedAt = clock.instant();
        var claims = JwtClaimsSet.builder()
                .subject(username)
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(lifetime))
                .build();
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
