package com.maxkb4j.trigger.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.maxkb4j.common.constant.LoginType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TriggerControllerTest {

    @Test
    void allTriggerManagementEndpointsRequireAdminLogin() {
        assertAdminLogin(TriggerController.class.getDeclaredMethods(), 12);
        assertAdminLogin(TriggerTaskRecordController.class.getDeclaredMethods(), 2);
    }

    private static void assertAdminLogin(Method[] methods, int expectedCount) {
        List<Method> endpoints = Arrays.stream(methods)
                .filter(method -> !method.isSynthetic())
                .toList();
        assertThat(endpoints).hasSize(expectedCount);
        assertThat(endpoints).allSatisfy(method -> {
            SaCheckLogin login = method.getAnnotation(SaCheckLogin.class);
            assertThat(login).as(method.getName() + " login annotation").isNotNull();
            assertThat(login.type()).as(method.getName() + " login type").isEqualTo(LoginType.ADMIN);
        });
    }
}
