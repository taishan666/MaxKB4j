package com.maxkb4j.system.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.maxkb4j.common.constant.RoleConst;
import com.maxkb4j.system.entity.UserEntity;
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
    void activeUser_loadsRoleAndPermissions() {
        UserEntity user = new UserEntity();
        user.setId("user-1");
        user.setRole(RoleConst.USER);
        user.setIsActive(true);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(userResourcePermissionService.getByUserId("user-1")).thenReturn(List.of());

        assertThat(stpInterface.getRoleList("user-1", "ADMIN")).containsExactly(RoleConst.USER);
        assertThat(stpInterface.getPermissionList("user-1", "ADMIN")).isNotEmpty();
    }
}
