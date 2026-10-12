package com.xaxage.be.adapter.in.web;

import com.xaxage.be.application.port.out.PokeApiUnavailableException;
import com.xaxage.be.domain.exception.PokemonNotFoundException;
import com.xaxage.be.domain.exception.PokemonNotStoredException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Gives every error the same body: an RFC 9457 problem detail ({@code application/problem+json}).
 */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * Says only that the data is unavailable; what went wrong upstream stays out of the response.
     */
    @ExceptionHandler(PokeApiUnavailableException.class)
    ProblemDetail pokeApiUnavailable(PokeApiUnavailableException unavailable) {
        logger.warn("PokeAPI is unavailable", unavailable);
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "Pokémon data is temporarily unavailable. Please try again later.");
    }

    @ExceptionHandler(PokemonNotFoundException.class)
    ProblemDetail pokemonNotFound(PokemonNotFoundException notFound) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, notFound.getMessage());
    }

    /**
     * Says only that a valid token is needed; why the token was not accepted stays out of the response.
     */
    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail unauthenticated(AuthenticationException unauthenticated) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "A valid bearer token is required.");
    }

    @ExceptionHandler(PokemonNotStoredException.class)
    ProblemDetail pokemonNotStored(PokemonNotStoredException notStored) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, notStored.getMessage());
    }
}
