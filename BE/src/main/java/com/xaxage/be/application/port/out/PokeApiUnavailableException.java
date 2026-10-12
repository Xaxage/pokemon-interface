package com.xaxage.be.application.port.out;

/**
 * PokeAPI could not be reached or could not answer, so its data is not available right now.
 */
public class PokeApiUnavailableException extends RuntimeException {

    public PokeApiUnavailableException(Throwable cause) {
        super("PokeAPI is unavailable", cause);
    }
}
