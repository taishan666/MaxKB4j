package com.maxkb4j.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.maxkb4j.application.entity.ApplicationApiKeyEntity;
import com.maxkb4j.application.mapper.ApplicationApiKeyMapper;
import com.maxkb4j.common.context.UserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationApiKeyServiceImplTest {

    private static final String APP_ID = "app-1";
    private static final String KEY_ID = "key-1";

    @Mock
    private UserContext userContext;
    @Mock
    private ApplicationApiKeyMapper apiKeyMapper;

    private ApplicationApiKeyServiceImpl apiKeyService;

    @BeforeEach
    void setUp() {
        apiKeyService = new ApplicationApiKeyServiceImpl(userContext);
        ReflectionTestUtils.setField(apiKeyService, "baseMapper", apiKeyMapper);
    }

    @Test
    void updateApikey_clearsEntityAppId() {
        ApplicationApiKeyEntity entity = new ApplicationApiKeyEntity();
        entity.setSecretKey("new-secret");
        when(apiKeyMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        Boolean result = apiKeyService.updateApikey(APP_ID, KEY_ID, entity);

        assertTrue(result);
        assertNull(entity.getApplicationId());
    }

    @Test
    void deleteApikey_includesAppInWhere() {
        when(apiKeyMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);

        Boolean result = apiKeyService.deleteApikey(APP_ID, KEY_ID);

        assertTrue(result);
    }

    @Test
    void deleteApikey_wrongApp_returnsFalse() {
        when(apiKeyMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(0);

        Boolean result = apiKeyService.deleteApikey(APP_ID, KEY_ID);

        assertFalse(result);
    }
}
