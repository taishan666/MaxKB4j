package com.maxkb4j.start.listener;

import cn.dev33.satoken.config.SaTokenConfig;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.enums.SettingType;
import com.maxkb4j.system.service.SystemSettingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.env.MockEnvironment;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtSecretInitializerTest {

    @Mock
    private SystemSettingService systemSettingService;
    @Mock
    private SaTokenConfig saTokenConfig;

    private MockEnvironment environment;
    private JwtSecretInitializer initializer;

    @BeforeEach
    void setUp() {
        environment = new MockEnvironment();
        initializer = new JwtSecretInitializer(systemSettingService, environment, saTokenConfig);
    }

    @Test
    void initialize_firstStartupGeneratesAndPersistsSecret() {
        when(systemSettingService.getSettingMeta(SettingType.JWT_SECRET)).thenReturn(null);
        when(systemSettingService.saveOrUpdate(any(JSONObject.class), any(SettingType.class))).thenReturn(true);

        initializer.initialize();

        ArgumentCaptor<JSONObject> metaCaptor = ArgumentCaptor.forClass(JSONObject.class);
        verify(systemSettingService).saveOrUpdate(metaCaptor.capture(), any(SettingType.class));
        String secret = metaCaptor.getValue().getString("jwtSecret");
        assertThat(Base64.getUrlDecoder().decode(secret)).hasSize(32);
        verify(saTokenConfig).setJwtSecretKey(secret);
    }

    @Test
    void initialize_reusesPersistedSecret() {
        JSONObject meta = new JSONObject();
        meta.put("jwtSecret", "persisted-secret");
        when(systemSettingService.getSettingMeta(SettingType.JWT_SECRET)).thenReturn(meta);

        initializer.initialize();

        verify(saTokenConfig).setJwtSecretKey("persisted-secret");
        verify(systemSettingService, never()).saveOrUpdate(any(JSONObject.class), any(SettingType.class));
    }

    @Test
    void initialize_explicitConfigurationTakesPrecedence() {
        environment.setProperty("sa-token.jwt-secret-key", "configured-secret");

        initializer.initialize();

        verifyNoInteractions(systemSettingService);
        verify(saTokenConfig, never()).setJwtSecretKey(any());
    }

    @Test
    void initialize_persistenceFailureStopsStartup() {
        when(systemSettingService.getSettingMeta(SettingType.JWT_SECRET)).thenReturn(null);
        when(systemSettingService.saveOrUpdate(any(JSONObject.class), any(SettingType.class))).thenReturn(false);

        assertThatThrownBy(initializer::initialize)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT 密钥持久化失败");
        verify(saTokenConfig, never()).setJwtSecretKey(any());
    }
}
