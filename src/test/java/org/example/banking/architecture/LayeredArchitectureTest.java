package org.example.banking.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@AnalyzeClasses(packages = "org.example.banking")
class LayeredArchitectureTest {

    @ArchTest
    static final ArchRule repositoriesAreOnlyAccessedThroughServices = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Controllers").definedBy("org.example.banking.controllers..")
            .layer("Services").definedBy("org.example.banking.services..")
            .layer("Repositories").definedBy("org.example.banking.repositories..")
            .whereLayer("Repositories").mayOnlyBeAccessedByLayers("Services");
}
