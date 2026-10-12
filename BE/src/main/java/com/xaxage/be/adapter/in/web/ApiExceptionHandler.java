package com.xaxage.be.adapter.in.web;

import com.xaxage.be.application.port.out.PokeApiUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
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
}
