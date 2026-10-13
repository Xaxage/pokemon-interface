package com.xaxage.be.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.UniqueElements;

import java.util.List;

/**
 * Our own fields of a stored Pokémon, as the reader wants them; any left out are cleared.
 */
public record EditStoredPokemonRequest(
        @Pattern(regexp = NOT_BLANK, message = "must not be blank") @Size(max = 50) String localizedName,
        @Pattern(regexp = NOT_BLANK, message = "must not be blank") @Size(max = 50) String region,
        @Size(max = 10) @UniqueElements List<@NotBlank @Size(max = 30) String> tags) {

    /**
     * Holds at least one character that is not whitespace; a value left out is not checked.
     */
    private static final String NOT_BLANK = "(?s).*\\S.*";
}
