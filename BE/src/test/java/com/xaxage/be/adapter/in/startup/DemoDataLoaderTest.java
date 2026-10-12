package com.xaxage.be.adapter.in.startup;

import com.xaxage.be.application.port.in.PopulateDemoDataUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@DisplayName("Loading demo data when the app starts")
class DemoDataLoaderTest {

    @Nested
    @DisplayName("Must come pre-populated with demo data")
    class ComesPrePopulatedWithDemoData {

        @Test
        @DisplayName("The one where the app starts, so the local database is populated with demo data")
        void populatesDemoDataWhenTheAppStarts() {
            var populateDemoData = mock(PopulateDemoDataUseCase.class);

            new DemoDataLoader(populateDemoData).run(new DefaultApplicationArguments());

            then(populateDemoData).should().populate();
        }
    }
}
