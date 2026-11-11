package com.aptisprep;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

@AnalyzeClasses(
        packages = "com.aptisprep.mvc",
        importOptions = {ImportOption.DoNotIncludeTests.class})
public class ArchitectureTests {

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
                    .resideInAPackage("..service.impl..")
                    .as("@Service must only be placed on *ServiceImpl classes in ..service.impl..");

    @ArchTest
    static final ArchRule service_interfaces_must_have_no_default_methods =
            methods()
                    .that()
                    .areDeclaredInClassesThat()
                    .resideInAPackage("..service")
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
    static final ArchRule feature_repository_isolation =
            classes()
                    .that()
                    .resideInAPackage("com.aptisprep.mvc..")
                    .should(
                            new ArchCondition<JavaClass>(
                                    "not depend on repositories of other features") {
                                @Override
                                public void check(JavaClass javaClass, ConditionEvents events) {
                                    String className = javaClass.getName();
                                    if (!className.startsWith("com.aptisprep.mvc.")) return;
                                    String sub = className.substring("com.aptisprep.mvc.".length());
                                    int dot = sub.indexOf('.');
                                    if (dot < 0) return;
                                    String currentFeature = sub.substring(0, dot);
                                    if ("common".equals(currentFeature)) return;

                                    javaClass
                                            .getDirectDependenciesFromSelf()
                                            .forEach(
                                                    dep -> {
                                                        JavaClass target = dep.getTargetClass();
                                                        String targetPkg = target.getPackageName();
                                                        if (targetPkg.startsWith(
                                                                        "com.aptisprep.mvc.")
                                                                && targetPkg.contains(
                                                                        ".repository")) {
                                                            String targetSub =
                                                                    targetPkg.substring(
                                                                            "com.aptisprep.mvc."
                                                                                    .length());
                                                            int targetDot = targetSub.indexOf('.');
                                                            if (targetDot > 0) {
                                                                String targetFeature =
                                                                        targetSub.substring(
                                                                                0, targetDot);
                                                                if (!targetFeature.equals(
                                                                                currentFeature)
                                                                        && !"common"
                                                                                .equals(
                                                                                        targetFeature)) {
                                                                    events.add(
                                                                            SimpleConditionEvent
                                                                                    .violated(
                                                                                            javaClass,
                                                                                            className
                                                                                                    + " in feature '"
                                                                                                    + currentFeature
                                                                                                    + "' depends on repository "
                                                                                                    + target
                                                                                                            .getName()
                                                                                                    + " in feature '"
                                                                                                    + targetFeature
                                                                                                    + "'"));
                                                                }
                                                            }
                                                        }
                                                    });
                                }
                            })
                    .as("Classes in one feature must not depend on repositories of other features");
}
