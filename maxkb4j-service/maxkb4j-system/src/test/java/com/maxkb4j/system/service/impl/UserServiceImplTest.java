package com.maxkb4j.system.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.maxkb4j.common.cache.UserTokenRevokeCache;
import com.maxkb4j.common.constant.RoleConst;
import com.maxkb4j.common.exception.ApiException;
import com.maxkb4j.common.props.SystemProperties;
import com.maxkb4j.system.cache.AuthCodeCache;
import com.maxkb4j.system.entity.UserEntity;
import com.maxkb4j.system.mapper.UserMapper;
import com.maxkb4j.system.security.LoginAttemptLimiter;
import com.maxkb4j.system.security.PasswordService;
import com.maxkb4j.system.service.EmailService;
import com.maxkb4j.system.service.IUserResourcePermissionInternalService;
import com.maxkb4j.user.dto.PasswordDTO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 用户删除防护与令牌撤销逻辑测试。
 */
class UserServiceImplTest {

    private static final String USER_ID = "user-1";
    private static final String ADMIN_ID = "admin-1";

    private EmailService emailService;
    private PasswordService passwordService;
    private IUserResourcePermissionInternalService userResourcePermissionService;
    private UserTokenRevokeCache tokenRevokeCache;
    private UserMapper userMapper;
    private UserServiceImpl userService;

    @BeforeAll
    static void initTableInfo() {
        // 纯 Mockito 环境下 MyBatis-Plus 的 LambdaWrapper 解析列名依赖 TableInfo 缓存，需手动初始化
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        if (TableInfoHelper.getTableInfo(UserEntity.class) == null) {
            TableInfoHelper.initTableInfo(assistant, UserEntity.class);
        }
    }

    @BeforeEach
    void setUp() {
        emailService = mock(EmailService.class);
        passwordService = mock(PasswordService.class);
        userResourcePermissionService = mock(IUserResourcePermissionInternalService.class);
        tokenRevokeCache = mock(UserTokenRevokeCache.class);
        userMapper = mock(UserMapper.class);
        userService = new UserServiceImpl(
                emailService,
                mock(cn.dev33.satoken.stp.StpInterface.class),
                new SystemProperties(),
                mock(com.maxkb4j.common.context.UserContext.class),
                passwordService,
                mock(LoginAttemptLimiter.class),
                mock(AuthCodeCache.class),
                userResourcePermissionService,
                tokenRevokeCache);
        ReflectionTestUtils.setField(userService, "baseMapper", userMapper);
        // IRepository.lambdaQuery() 经 getEntityClass() 解析实体类型时依赖真实 MybatisMapperProxy，
        // Mockito mock mapper 走不通该反射路径，预置实体类型短路掉
        ReflectionTestUtils.setField(userService, "entityClass", UserEntity.class);
    }

    private UserEntity user(String id, String role) {
        UserEntity entity = new UserEntity();
        entity.setId(id);
        entity.setRole(role);
        return entity;
    }

    // ============ deleteUserById 删除防护 ============

    @Test
    void deleteUserById_missing_returnsFalse() {
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThat(userService.deleteUserById(USER_ID)).isFalse();
        verify(userResourcePermissionService, never()).removeByUserIds(any());
        verify(userMapper, never()).deleteApplicationApiKeyByUserIds(any());
        verify(tokenRevokeCache, never()).revoke(anyString());
    }

    @Test
    void deleteUserById_adminRole_throws() {
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user(ADMIN_ID, RoleConst.ADMIN));

        assertThatThrownBy(() -> userService.deleteUserById(ADMIN_ID))
                .isInstanceOf(ApiException.class)
                .hasMessage("user.delete.admin.forbidden");
        // 管理员不可删除，未执行任何清理与删除动作
        verify(userResourcePermissionService, never()).removeByUserIds(any());
        verify(userMapper, never()).deleteApplicationApiKeyByUserIds(any());
        verify(tokenRevokeCache, never()).revoke(anyString());
    }

    @Test
    void deleteUserById_ownedResource_throws() {
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user(USER_ID, RoleConst.USER));
        when(userMapper.countOwnedResource(USER_ID)).thenReturn(1L);

        assertThatThrownBy(() -> userService.deleteUserById(USER_ID))
                .isInstanceOf(ApiException.class)
                .hasMessage("user.delete.resource.exists");
        verify(userResourcePermissionService, never()).removeByUserIds(any());
        verify(userMapper, never()).deleteApplicationApiKeyByUserIds(any());
        verify(tokenRevokeCache, never()).revoke(anyString());
    }

    @Test
    void deleteUserById_success_cleansReferencesAndRevokes() {
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user(USER_ID, RoleConst.USER));
        when(userMapper.countOwnedResource(USER_ID)).thenReturn(0L);
        // 显式按 Serializable 重载打桩：BaseMapper 存在 deleteById(Serializable)/deleteById(T) 双重载
        when(userMapper.deleteById(any(java.io.Serializable.class))).thenReturn(1);

        boolean removed = userService.deleteUserById(USER_ID);

        assertThat(removed).isTrue();
        verify(userResourcePermissionService).removeByUserIds(List.of(USER_ID));
        verify(userMapper).deleteApplicationApiKeyByUserIds(List.of(USER_ID));
        verify(tokenRevokeCache).revoke(USER_ID);
    }

    // ============ batchDelete 批量删除防护 ============

    @Test
    void batchDelete_mixedRoles_onlyDeletesNormalUsers() {
        // eq(role, USER) 过滤在 SQL 侧完成，mock 按数据库语义仅返回 USER 角色成员
        when(userMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(user(USER_ID, RoleConst.USER)));
        when(userMapper.countOwnedResource(USER_ID)).thenReturn(0L);
        when(userMapper.deleteById(any(java.io.Serializable.class))).thenReturn(1);

        boolean removed = userService.batchDelete(List.of(USER_ID, ADMIN_ID));

        assertThat(removed).isTrue();
        // 管理员账号被静默跳过，仅清理普通成员
        verify(userMapper).deleteApplicationApiKeyByUserIds(List.of(USER_ID));
        ArgumentCaptor<List<String>> idsCaptor = ArgumentCaptor.forClass(List.class);
        verify(tokenRevokeCache).revokeAll(idsCaptor.capture());
        assertThat(idsCaptor.getValue()).containsExactly(USER_ID);
    }

    @Test
    void batchDelete_ownedResource_throws() {
        when(userMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(user(USER_ID, RoleConst.USER)));
        when(userMapper.countOwnedResource(USER_ID)).thenReturn(1L);

        assertThatThrownBy(() -> userService.batchDelete(List.of(USER_ID)))
                .isInstanceOf(ApiException.class)
                .hasMessage("user.delete.resource.exists");
        verify(userMapper, never()).deleteById(any());
        verify(tokenRevokeCache, never()).revokeAll(anyCollection());
    }

    @Test
    void batchDelete_emptyIds_returnsFalse() {
        assertThat(userService.batchDelete(List.of())).isFalse();
        assertThat(userService.batchDelete(null)).isFalse();
    }

    // ============ 禁用/改密撤销令牌 ============

    @Test
    void updateUser_disabled_revokesToken() {
        UserEntity user = user(USER_ID, RoleConst.USER);
        user.setIsActive(false);
        when(userMapper.updateById(any(UserEntity.class))).thenReturn(1);

        boolean updated = userService.updateUser(user);

        assertThat(updated).isTrue();
        verify(tokenRevokeCache).revoke(USER_ID);
    }

    @Test
    void updateUser_enabled_doesNotRevoke() {
        UserEntity user = user(USER_ID, RoleConst.USER);
        user.setIsActive(true);
        when(userMapper.updateById(any(UserEntity.class))).thenReturn(1);

        userService.updateUser(user);

        verify(tokenRevokeCache, never()).revoke(anyString());
    }

    @Test
    void updatePassword_success_revokesToken() {
        when(userMapper.updateById(any(UserEntity.class))).thenReturn(1);
        when(passwordService.encode(anyString())).thenReturn("encoded");
        PasswordDTO dto = new PasswordDTO();
        dto.setPassword("new-password");
        dto.setRePassword("new-password");

        boolean updated = userService.updatePassword(USER_ID, dto);

        assertThat(updated).isTrue();
        verify(tokenRevokeCache).revoke(USER_ID);
    }
}
