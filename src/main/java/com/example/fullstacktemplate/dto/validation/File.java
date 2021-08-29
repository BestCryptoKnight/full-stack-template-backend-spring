package com.example.fullstacktemplate.dto.validation;


import com.example.fullstacktemplate.model.FileType;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({FIELD})
@Retention(RUNTIME)
@Constraint(validatedBy = FileValidator.class)
@Documented
public @interface File {

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String message();

    int maxSizeBytes();

    FileType[] fileTypes() default {};

}
