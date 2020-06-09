package de.tum.in.www1.artemis.validation.constraints;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import de.tum.in.www1.artemis.validation.ExamRequestValidator;

@Constraint(validatedBy = ExamRequestValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExamRequestConstraint {

    String message() default "{de.tum.in.www1.artemis.validation.constraints.ExamRequestConstraint}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
