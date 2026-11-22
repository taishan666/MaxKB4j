package com.maxkb4j.system.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.maxkb4j.common.constant.Permission;
import com.maxkb4j.common.constant.ResourceType;
import com.maxkb4j.common.constant.RoleConst;
import com.maxkb4j.common.enums.PermissionEnum;
import com.maxkb4j.system.entity.UserEntity;
import com.maxkb4j.system.entity.UserResourcePermissionEntity;
import com.maxkb4j.system.mapper.UserMapper;
import com.maxkb4j.system.service.IUserResourcePermissionInternalService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StpInterfaceImplTest {

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, UserEntity.class);
    }

    @Mock
    private UserMapper userMapper;

    @Mock
    private IUserResourcePermissionInternalService userResourcePermissionService;

    @InjectMocks
    private StpInterfaceImpl stpInterface;

    @Test
    void getRoleList_inactiveUser_returnsEmpty() {
        when(userMapper.selectOne(any())).thenReturn(null);

        assertThat(stpInterface.getRoleList("user-1", "ADMIN")).isEmpty();

        ArgumentCaptor<LambdaQueryWrapper<UserEntity>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(userMapper).selectOne(wrapperCaptor.capture());
        assertThat(wrapperCaptor.getValue().getSqlSegment()).contains("is_active");
        assertThat(wrapperCaptor.getValue().getParamNameValuePairs()).containsValue(true);
    }

    @Test
    void getPermissionList_inactiveUser_returnsEmptyWithoutLoadingPermissions() {
        when(userMapper.selectOne(any())).thenReturn(null);

        assertThat(stpInterface.getPermissionList("user-1", "ADMIN")).isEmpty();

        verifyNoInteractions(userResourcePermissionService);
    }

    @Test
    void activeUser_withoutGrants_returnsRoleOnly() {
        UserEntity user = new UserEntity();
        user.setId("user-1");
        user.setRole(RoleConst.USER);
        user.setIsActive(true);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(userResourcePermissionService.getByUserId("user-1")).thenReturn(List.of());

        assertThat(stpInterface.getRoleList("user-1", "ADMIN")).containsExactly(RoleConst.USER);
        // 默认权限兜底已移除，无授权记录的激活用户不再获得任何权限码
        assertThat(stpInterface.getPermissionList("user-1", "ADMIN")).isEmpty();
    }

    @Test
    void activeUser_loadsRoleAndPermissionsFromGrants() {
        UserEntity user = new UserEntity();
        user.setId("user-1");
        user.setRole(RoleConst.USER);
        user.setIsActive(true);
        when(userMapper.selectOne(any())).thenReturn(user);
        UserResourcePermissionEntity grant = new UserResourcePermissionEntity();
        grant.setAuthTargetType(ResourceType.APPLICATION);
        grant.setTargetId("app-1");
        grant.setPermissionList(List.of(Permission.VIEW));
        when(userResourcePermissionService.getByUserId("user-1")).thenReturn(List.of(grant));

        assertThat(stpInterface.getRoleList("user-1", "ADMIN")).containsExactly(RoleConst.USER);
        // VIEW 授权展开为 APPLICATION 资源类型下各分组的 READ 权限码，并挂到授权目标 app-1
        assertThat(stpInterface.getPermissionList("user-1", "ADMIN")).containsExactlyInAnyOrder(
                PermissionEnum.APPLICATION_READ.getResourcePerm("app-1"),
                PermissionEnum.APPLICATION_OVERVIEW_READ.getResourcePerm("app-1"),
                PermissionEnum.APPLICATION_CHAT_LOG_READ.getResourcePerm("app-1"),
                PermissionEnum.APPLICATION_ACCESS_READ.getResourcePerm("app-1"));
    }

    @Test
    void activeUser_manageGrant_createPermGlobalWithoutViewPerms() {
        UserEntity user = new UserEntity();
        user.setId("user-1");
        user.setRole(RoleConst.USER);
        user.setIsActive(true);
        when(userMapper.selectOne(any())).thenReturn(user);
        UserResourcePermissionEntity grant = new UserResourcePermissionEntity();
        grant.setAuthTargetType(ResourceType.APPLICATION);
        grant.setTargetId("app-1");
        grant.setPermissionList(List.of(Permission.MANAGE));
        when(userResourcePermissionService.getByUserId("user-1")).thenReturn(List.of(grant));

        List<String> permissions = stpInterface.getPermissionList("user-1", "ADMIN");
        // CREATE 操作不绑定授权目标，生成全局权限码
        assertThat(permissions).contains(PermissionEnum.APPLICATION_CREATE.getResourcePerm());
        // 仅授 MANAGE 时不应混入 VIEW 级别的 READ 权限码
        assertThat(permissions).doesNotContain(PermissionEnum.APPLICATION_READ.getResourcePerm("app-1"));
    }
}
