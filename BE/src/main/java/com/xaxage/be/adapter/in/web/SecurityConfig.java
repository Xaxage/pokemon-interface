package com.xaxage.be.adapter.in.web;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.HandlerExceptionResolver;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Synchronising, updating and deleting stored Pokémon need a signed-in user, shown by a valid bearer token;
 * everything else is open to anyone.
 */
@Configuration
class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationEntryPoint unauthorized)
            throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(HttpMethod.POST, "/api/stored-pokemon/*/sync").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/stored-pokemon/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/stored-pokemon/*").authenticated()
                        .anyRequest().permitAll())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(unauthorized))
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(unauthorized))
                .build();
    }

    /**
     * Answers with the same error body as every other endpoint, by handing the failure to the MVC exception handlers.
     */
    @Bean
    AuthenticationEntryPoint unauthorized(
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
        return (request, response, authenticationFailure) ->
                exceptionResolver.resolveException(request, response, null, authenticationFailure);
    }

    /**
     * Tokens are signed with HS256 using the configured secret, and stop being accepted the moment they expire:
     * no clock skew is allowed, as the tokens are issued by this same app.
     */
    @Bean
    JwtDecoder jwtDecoder(@Value("${security.jwt.secret}") String secret) {
        var decoder = NimbusJwtDecoder
                .withSecretKey(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
                .build();
        decoder.setJwtValidator(new JwtTimestampValidator(Duration.ZERO));
        return decoder;
    }
}
