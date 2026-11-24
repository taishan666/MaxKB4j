package com.maxkb4j.trigger.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.maxkb4j.common.annotation.SaCheckPerm;
import com.maxkb4j.common.constant.LoginType;
import com.maxkb4j.common.constant.RoleConst;
import com.maxkb4j.common.enums.PermissionEnum;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TriggerControllerTest {

    /**
     * 资源维度触发器接口的期望权限：方法名 -> 权限枚举。
     * 这些接口不对角色做限制，授权由 SaCheckPermAspect 按资源级权限串（资源组:操作:/WORKSPACE/default/资源类型/{id}）校验
     */
    private static final Map<String, PermissionEnum> SOURCE_ENDPOINT_PERMS = Map.ofEntries(
            Map.entry("addToolTrigger", PermissionEnum.TOOL_TRIGGER_CREATE),
            Map.entry("addApplicationTrigger", PermissionEnum.APPLICATION_TRIGGER_CREATE),
            Map.entry("getToolTrigger", PermissionEnum.TOOL_TRIGGER_READ),
            Map.entry("getApplicationTrigger", PermissionEnum.APPLICATION_TRIGGER_READ),
            Map.entry("updateToolTrigger", PermissionEnum.TOOL_TRIGGER_EDIT),
            Map.entry("updateApplicationTrigger", PermissionEnum.APPLICATION_TRIGGER_EDIT),
            Map.entry("listToolTriggers", PermissionEnum.TOOL_TRIGGER_READ),
            Map.entry("listApplicationTriggers", PermissionEnum.APPLICATION_TRIGGER_READ),
            Map.entry("deleteToolTrigger", PermissionEnum.TOOL_TRIGGER_DELETE),
            Map.entry("deleteApplicationTrigger", PermissionEnum.APPLICATION_TRIGGER_DELETE));

    @Test
    void triggerManagementEndpointsRequireAdminRole() {
        assertAdminRole(endpointsOf(TriggerTaskRecordController.class));
        List<Method> managementEndpoints = endpointsOf(TriggerController.class).stream()
                .filter(method -> method.getAnnotation(SaCheckPerm.class) == null)
                .toList();
        assertThat(managementEndpoints).hasSize(7);
        assertAdminRole(managementEndpoints);
    }

    @Test
    void sourceScopedTriggerEndpointsCheckResourcePermission() {
        List<Method> sourceEndpoints = endpointsOf(TriggerController.class).stream()
                .filter(method -> method.getAnnotation(SaCheckPerm.class) != null)
                .toList();
        assertThat(sourceEndpoints).hasSize(SOURCE_ENDPOINT_PERMS.size());
        sourceEndpoints.forEach(method -> {
            PermissionEnum permission = method.getAnnotation(SaCheckPerm.class).value();
            assertThat(permission).as(method.getName() + " permission")
                    .isEqualTo(SOURCE_ENDPOINT_PERMS.get(method.getName()));
            String[] paths = mappingPaths(method);
            assertThat(paths[0]).as(method.getName() + " path prefix")
                    .startsWith("/" + permission.getResourceType());
            assertThat(String.join("", paths)).as(method.getName() + " path carries {id} for the aspect to resolve")
                    .contains("{id}");
        });
    }

    private static void assertAdminRole(List<Method> methods) {
        assertThat(methods).allSatisfy(method -> {
            SaCheckRole role = method.getAnnotation(SaCheckRole.class);
            assertThat(role).as(method.getName() + " role annotation").isNotNull();
            assertThat(role.type()).as(method.getName() + " role type").isEqualTo(LoginType.ADMIN);
            assertThat(role.value()).as(method.getName() + " role value").containsExactly(RoleConst.ADMIN);
        });
    }

    private static List<Method> endpointsOf(Class<?> controller) {
        return Arrays.stream(controller.getDeclaredMethods())
                .filter(method -> !method.isSynthetic())
                .filter(TriggerControllerTest::isEndpoint)
                .toList();
    }

    private static boolean isEndpoint(Method method) {
        return Arrays.stream(method.getAnnotations())
                .anyMatch(annotation -> annotation.annotationType().isAnnotationPresent(RequestMapping.class));
    }

    private static String[] mappingPaths(Method method) {
        for (Annotation annotation : method.getAnnotations()) {
            String[] paths = switch (annotation) {
                case org.springframework.web.bind.annotation.GetMapping m -> m.value().length > 0 ? m.value() : m.path();
                case org.springframework.web.bind.annotation.PostMapping m -> m.value().length > 0 ? m.value() : m.path();
                case org.springframework.web.bind.annotation.PutMapping m -> m.value().length > 0 ? m.value() : m.path();
                case org.springframework.web.bind.annotation.DeleteMapping m -> m.value().length > 0 ? m.value() : m.path();
                default -> null;
            };
            if (paths != null) {
                return paths;
            }
        }
        throw new AssertionError(method.getName() + " has no mapping annotation");
    }

}
