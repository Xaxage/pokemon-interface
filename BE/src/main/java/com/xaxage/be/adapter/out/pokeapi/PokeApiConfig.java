package com.xaxage.be.adapter.out.pokeapi;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
class PokeApiConfig {

    static final String POKE_API_REST_CLIENT = "pokeApiRestClient";

    @Bean(POKE_API_REST_CLIENT)
    RestClient pokeApiRestClient(@Value("${pokeapi.base-url}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }
}