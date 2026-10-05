package io.miragon.common.architecture.condition;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaParameter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.List;

public final class UseCaseDependencyConditions {

    private UseCaseDependencyConditions() {
    }

    /**
     * Ensures that a class does not depend on multiple classes from the application package.
     * This enforces that an adapter can only fulfill one use-case.
     * Moreover, it prevents performance issues by ensuring only one transaction is open,
     * as transactions are managed on the application layer.
     *
     * @return ArchCondition that can be used in {@code .should(...)}
     */
    public static ArchCondition<JavaClass> onlyFulfilOneUseCase() {
        return new ArchCondition<>("only fulfil one use case") {

            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                List<JavaParameter> applicationDependencies = findUseCaseDependenciesInConstructor(javaClass);
                if (applicationDependencies.size() > 1) {
                    String message = javaClass.getName() + " depends on more than one use-case: "
                            + applicationDependencies;
                    events.add(SimpleConditionEvent.violated(javaClass, message));
                }
            }

            /**
             * Finds all constructor parameters that are from the application package,
             * which typically represent use case dependencies.
             */
            private List<JavaParameter> findUseCaseDependenciesInConstructor(JavaClass javaClass) {
                return javaClass.getConstructors().stream()
                        .flatMap(constructor -> constructor.getParameters().stream())
                        .filter(param -> param.getRawType().getPackageName().contains("application.port.inbound"))
                        .toList();
            }
        };
    }
}
