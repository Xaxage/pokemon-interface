package com.xaxage.be.application.port.in;

public interface ProvideDemoUserUseCase {

    /**
     * Registers the demo user, unless it is registered already.
     */
    void provide();
}
