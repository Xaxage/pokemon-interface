package com.xaxage.be.adapter.in.startup;

import com.xaxage.be.application.port.in.PopulateDemoDataUseCase;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Gives the local database its demo data once the app has started.
 */
@Component
class DemoDataLoader implements ApplicationRunner {

    private final PopulateDemoDataUseCase populateDemoData;

    DemoDataLoader(PopulateDemoDataUseCase populateDemoData) {
        this.populateDemoData = populateDemoData;
    }

    @Override
    public void run(ApplicationArguments args) {
        populateDemoData.populate();
    }
}
