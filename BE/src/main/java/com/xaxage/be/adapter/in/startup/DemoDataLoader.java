package com.xaxage.be.adapter.in.startup;

import com.xaxage.be.application.port.in.PopulateDemoDataUseCase;
import com.xaxage.be.application.port.in.ProvideDemoUserUseCase;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Gives the local database its demo data and demo user once the app has started.
 */
@Component
class DemoDataLoader implements ApplicationRunner {

    private final PopulateDemoDataUseCase populateDemoData;
    private final ProvideDemoUserUseCase provideDemoUser;

    DemoDataLoader(PopulateDemoDataUseCase populateDemoData, ProvideDemoUserUseCase provideDemoUser) {
        this.populateDemoData = populateDemoData;
        this.provideDemoUser = provideDemoUser;
    }

    @Override
    public void run(ApplicationArguments args) {
        populateDemoData.populate();
        provideDemoUser.provide();
    }
}
