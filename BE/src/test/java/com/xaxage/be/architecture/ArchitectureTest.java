package com.xaxage.be.architecture;

import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Enforces the hexagonal architecture and boundary rules declared in CLAUDE.md.
 *
 * <p>The layer packages ({@code domain}, {@code application}, {@code adapter}) may not all
 * exist yet while the solution is being built out via ATDD, so the rules are configured to
 * tolerate empty / optional layers rather than failing before the code lands.
 */
@AnalyzeClasses(packages = "com.xaxage.be", importOptions = DoNotIncludeTests.class)
@DisplayName("Architecture and boundary rules (hexagonal)")
class ArchitectureTest {

    @ArchTest
    static final ArchRule domain_must_not_depend_on_spring =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage("org.springframework..")
                    .as("the domain must not depend on Spring")
                    .because("the domain must stay free of framework dependencies (CLAUDE.md)")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule domain_must_not_depend_on_jpa =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage("jakarta.persistence..")
                    .as("the domain must not depend on jakarta.persistence")
                    .because("persistence concerns belong to the outbound adapter, not the domain (CLAUDE.md)")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule dependencies_flow_inward =
            layeredArchitecture().consideringAllDependencies()
                    .layer("Adapter").definedBy("..adapter..")
                    .layer("Application").definedBy("..application..")
                    .layer("Domain").definedBy("..domain..")

                    // adapter -> application -> domain: inner layers never reach outward.
                    .whereLayer("Adapter").mayNotBeAccessedByAnyLayer()
                    .whereLayer("Application").mayOnlyBeAccessedByLayers("Adapter")
                    .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Adapter")

                    .withOptionalLayers(true)
                    .as("dependencies must flow inward: adapter -> application -> domain")
                    .allowEmptyShould(true);
}
