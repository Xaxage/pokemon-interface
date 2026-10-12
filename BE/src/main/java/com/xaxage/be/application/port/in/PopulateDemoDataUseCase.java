package com.xaxage.be.application.port.in;

public interface PopulateDemoDataUseCase {

    /**
     * Stores the demo Pokémon, unless Pokémon are stored already.
     */
    void populate();
}
