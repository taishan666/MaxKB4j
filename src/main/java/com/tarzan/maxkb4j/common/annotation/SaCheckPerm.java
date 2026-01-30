package com.tarzan.maxkb4j.common.annotation;

import com.tarzan.maxkb4j.module.system.user.enums.PermissionEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SaCheckPerm {
    PermissionEnum value();
}