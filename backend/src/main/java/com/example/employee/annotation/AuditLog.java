package com.example.employee.annotation;

import com.example.employee.common.OperationType;
import com.example.employee.common.TargetModule;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {

    TargetModule module();

    OperationType operation();

    String recordIdParam() default "id";

    String recordNameField() default "name";
}
