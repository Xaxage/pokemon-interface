package com.xaxage.be.adapter.in.web;

import com.xaxage.be.application.port.out.PokeApiUnavailableException;
import com.xaxage.be.domain.exception.PokemonNotFoundException;
import com.xaxage.be.domain.exception.PokemonNotStoredException;
import com.xaxage.be.domain.exception.UsernameTakenException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
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

    @ExceptionHandler(UsernameTakenException.class)
    ProblemDetail usernameTaken(UsernameTakenException taken) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, taken.getMessage());
    }

    /**
     * Keeps the standard error body, and lists each field of the request body that is not valid, with why.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException invalid,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        var problem = invalid.getBody();
        problem.setProperty("invalidFields", invalid.getBindingResult().getFieldErrors().stream()
                .map(error -> new InvalidField(error.getField(), error.getDefaultMessage()))
                .toList());
        return handleExceptionInternal(invalid, problem, headers, status, request);
    }

    /**
     * @param field  the field of the request body, as the reader sent it
     * @param reason why its value is not valid
     */
    record InvalidField(String field, String reason) {
    }
}
