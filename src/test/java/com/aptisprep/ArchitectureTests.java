package com.aptisprep;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Entity;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

@AnalyzeClasses(
        packages = "com.aptisprep",
        importOptions = {ImportOption.DoNotIncludeTests.class})
public class ArchitectureTests {

    @ArchTest
    static final ArchRule mvc_packages_must_not_return =
            noClasses()
                    .should()
                    .resideInAPackage("..mvc..")
                    .as("The legacy feature-first mvc package must remain removed");

    @ArchTest
    static final ArchRule page_controllers_must_be_grouped_by_actor =
            classes()
                    .that()
                    .areAnnotatedWith(Controller.class)
                    .should()
                    .resideInAnyPackage(
                            "com.aptisprep.controller",
                            "com.aptisprep.controller.auth..",
                            "com.aptisprep.controller.admin..",
                            "com.aptisprep.controller.student..")
                    .as(
                            "Page controllers must be grouped by the actor whose views they coordinate");

    @ArchTest
    static final ArchRule jpa_entities_must_be_in_entity_package =
            classes()
                    .that()
                    .areAnnotatedWith(Entity.class)
                    .should()
                    .resideInAPackage("com.aptisprep.entity..")
                    .as("JPA entities must be kept in the entity layer");

    @ArchTest
    static final ArchRule dto_types_must_be_form_or_view_models =
            classes()
                    .that()
                    .haveSimpleNameEndingWith("DTO")
                    .should()
                    .resideInAnyPackage("com.aptisprep.dto.form..", "com.aptisprep.dto.view..")
                    .as("DTOs must be separated into form input and view output models");

    @ArchTest
    static final ArchRule controllers_must_not_depend_on_repositories =
            noClasses()
                    .that()
                    .resideInAPackage("..controller..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..repository..")
                    .as("Controllers must not depend on repositories");

    @ArchTest
    static final ArchRule controllers_must_not_depend_on_entities =
            noClasses()
                    .that()
                    .resideInAPackage("..controller..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..entity..")
                    .as("Controllers must not depend on entities");

    @ArchTest
    static final ArchRule controllers_must_not_depend_on_service_impls =
            noClasses()
                    .that()
                    .resideInAPackage("..controller..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..service.impl..")
                    .as("Controllers must not depend on service implementations");

    @ArchTest
    static final ArchRule service_annotation_only_on_service_impl =
            classes()
                    .that()
                    .areAnnotatedWith(Service.class)
                    .should()
                    .haveSimpleNameEndingWith("ServiceImpl")
                    .andShould()
                    .resideInAPackage("..service..impl..")
                    .as(
                            "@Service must only be placed on *ServiceImpl classes in a service impl package");

    @ArchTest
    static final ArchRule service_interfaces_must_have_no_default_methods =
            methods()
                    .that()
                    .areDeclaredInClassesThat()
                    .resideInAPackage("..service..")
                    .and()
                    .areDeclaredInClassesThat()
                    .areInterfaces()
                    .should(
                            new ArchCondition<JavaMethod>(
                                    "have no method body (no default or static methods with bodies)") {
                                @Override
                                public void check(JavaMethod method, ConditionEvents events) {
                                    if (method.reflect().isDefault()
                                            || !method.getModifiers()
                                                    .contains(JavaModifier.ABSTRACT)) {
                                        events.add(
                                                SimpleConditionEvent.violated(
                                                        method,
                                                        "Method "
                                                                + method.getFullName()
                                                                + " has a body in service interface"));
                                    }
                                }
                            })
                    .as("Service interfaces must not have method bodies (no default methods)");

    @ArchTest
    static final ArchRule no_rest_controller_annotation =
            noClasses()
                    .should()
                    .beAnnotatedWith(RestController.class)
                    .as("No class should be annotated with @RestController");

    @ArchTest
    static final ArchRule repositories_must_not_depend_on_upper_layers =
            noClasses()
                    .that()
                    .resideInAPackage("..repository..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("..controller..", "..service..", "..dto..")
                    .as("Repositories must only expose persistence types");

    @ArchTest
    static final ArchRule services_must_not_depend_on_controllers =
            noClasses()
                    .that()
                    .resideInAPackage("..service..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..controller..")
                    .as("Services must not depend on the web layer");
}
