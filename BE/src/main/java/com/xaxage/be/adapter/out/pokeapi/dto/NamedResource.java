package com.xaxage.be.adapter.out.pokeapi.dto;

/**
 * PokeAPI's {@code {name, url}} reference; the id is the last segment of the url.
 */
public record NamedResource(String name, String url) {

    public int id() {
        var trimmed = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        return Integer.parseInt(trimmed.substring(trimmed.lastIndexOf('/') + 1));
    }
}