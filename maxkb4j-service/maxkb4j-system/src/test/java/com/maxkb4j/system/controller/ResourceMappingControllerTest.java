package com.maxkb4j.system.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.maxkb4j.common.constant.LoginType;
import com.maxkb4j.common.exception.ApiException;
import com.maxkb4j.core.support.permission.DataPermissionScope;
import com.maxkb4j.core.support.permission.DataPermissionSupport;
import com.maxkb4j.system.service.IResourceMappingInternalService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.bind.annotation.GetMapping;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceMappingControllerTest {

    @Mock
    private IResourceMappingInternalService resourceMappingService;

    @Mock
    private DataPermissionSupport dataPermissionSupport;

    @InjectMocks
    private ResourceMappingController resourceMappingController;

    @Test
    void allMappingEndpointsRequireAdminLogin() {
        List<Method> endpoints = Arrays.stream(ResourceMappingController.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(GetMapping.class))
                .toList();

        assertThat(endpoints).hasSize(2);
        assertThat(endpoints).allSatisfy(method -> {
            SaCheckLogin login = method.getAnnotation(SaCheckLogin.class);
            assertThat(login).as(method.getName() + " login annotation").isNotNull();
            assertThat(login.type()).as(method.getName() + " login type").isEqualTo(LoginType.ADMIN);
        });
    }

    @Test
    void mappingResourcePage_rejectsUnauthorizedCenterResource() {
        when(dataPermissionSupport.resolve("APPLICATION"))
                .thenReturn(DataPermissionScope.limited(List.of("application-2")));

        assertThatThrownBy(() -> resourceMappingController.mappingResourcePage(
                "APPLICATION", "application-1", 1, 10, null, null, null))
                .isInstanceOf(ApiException.class);
        verifyNoInteractions(resourceMappingService);
    }

    @Test
    void resourceMappingPage_rejectsUnauthorizedCenterResource() {
        when(dataPermissionSupport.resolve("KNOWLEDGE")).thenReturn(DataPermissionScope.empty());

        assertThatThrownBy(() -> resourceMappingController.resourceMappingPage(
                "KNOWLEDGE", "knowledge-1", 1, 10, null, null, null))
                .isInstanceOf(ApiException.class);
        verifyNoInteractions(resourceMappingService);
    }

    @Test
    void mappingResourcePage_allowsAuthorizedCenterResource() {
        String[] sourceTypes = {"TOOL"};
        when(dataPermissionSupport.resolve("MODEL"))
                .thenReturn(DataPermissionScope.limited(List.of("model-1")));

        resourceMappingController.mappingResourcePage(
                "MODEL", "model-1", 1, 10, "model", "owner", sourceTypes);

        verify(resourceMappingService).selectDependOnPage(
                "MODEL", "model-1", 1, 10, "model", "owner", sourceTypes);
    }

    @Test
    void resourceMappingPage_allowsAdmin() {
        when(dataPermissionSupport.resolve("TOOL")).thenReturn(DataPermissionScope.admin());

        resourceMappingController.resourceMappingPage(
                "TOOL", "tool-1", 2, 20, null, null, null);

        verify(resourceMappingService).selectBeDependedOnPage(
                "TOOL", "tool-1", 2, 20, null, null, null);
    }
}
