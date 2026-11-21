package com.maxkb4j.model.service.impl;

import com.baomidou.mybatisplus.extension.repository.AbstractRepository;
import com.baomidou.mybatisplus.extension.repository.CrudRepository;
import com.maxkb4j.common.context.UserContext;
import com.maxkb4j.common.util.DataMaskUtil;
import com.maxkb4j.model.entity.ModelCredential;
import com.maxkb4j.model.entity.ModelEntity;
import com.maxkb4j.model.mapper.ModelMapper;
import com.maxkb4j.model.provider.AbsModelProvider;
import com.maxkb4j.model.registry.ModelProviderRegistry;
import com.maxkb4j.system.constant.AuthTargetType;
import com.maxkb4j.user.service.IUserResourcePermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ModelServiceImpl#updateModel(String, ModelEntity)} 凭证回退逻辑测试：
 * 未传 credential、回传脱敏 apiKey、回传新 apiKey 三条路径均不应 NPE 且密钥不被脱敏值覆盖。
 * 另含 {@link ModelServiceImpl#createModel(ModelEntity)} 事务边界测试：
 * 远程校验必须发生在事务开启之前，避免长校验占死数据库连接。
 */
@ExtendWith(MockitoExtension.class)
class ModelServiceImplTest {

    private static final String MODEL_ID = "model-1";
    private static final String USER_ID = "user-1";
    private static final String STORED_API_KEY = "sk-1234567890abcdef";

    @Mock
    private ModelMapper modelMapper;
    @Mock
    private UserContext userContext;
    @Mock
    private ModelProviderRegistry providerRegistry;
    @Mock
    private AbsModelProvider provider;
    @Mock
    private IUserResourcePermissionService userResourcePermissionService;
    @Mock
    private PlatformTransactionManager transactionManager;

    private ModelServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        service = new ModelServiceImpl(userResourcePermissionService, userContext, null, providerRegistry, transactionManager);
        // ServiceImpl 的 getById/updateById/lambdaQuery 均走 baseMapper，注入 mock 即可脱离数据库
        setField(CrudRepository.class, service, "baseMapper", modelMapper);
        // 3.5.9 起实体类经由 MybatisMapperProxy 反推，mock mapper 推不出来，直接预置缓存字段
        setField(AbstractRepository.class, service, "entityClass", ModelEntity.class);
        setField(AbstractRepository.class, service, "mapperClass", ModelMapper.class);
        // @PostConstruct 在单测中不会执行，手工注入模板
        setField(ModelServiceImpl.class, service, "transactionTemplate", new TransactionTemplate(transactionManager));
    }

    private static void setField(Class<?> declaringClass, Object target, String name, Object value) throws Exception {
        Field field = declaringClass.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void updateModel_credentialMissing_fallsBackToStoredCredential() {
        ModelEntity stored = storedModel();
        when(userContext.getUserId()).thenReturn(USER_ID);
        when(modelMapper.selectCount(any())).thenReturn(0L);
        when(modelMapper.selectById(MODEL_ID)).thenReturn(stored);
        when(providerRegistry.get("OpenAI")).thenReturn(provider);
        when(modelMapper.updateById(any(ModelEntity.class))).thenReturn(1);

        ModelEntity request = requestModel();
        request.setCredential(null);

        ModelEntity result = service.updateModel(MODEL_ID, request);

        assertNotNull(result.getCredential());
        assertSame(stored.getCredential(), result.getCredential());
        assertEquals(STORED_API_KEY, result.getCredential().getApiKey());
        verify(provider).modelIsValid(eq("LLM"), eq("gpt-4o"), same(stored.getCredential()), any());
    }

    @Test
    void updateModel_maskedApiKeySubmitted_keepsStoredApiKey() {
        ModelEntity stored = storedModel();
        when(userContext.getUserId()).thenReturn(USER_ID);
        when(modelMapper.selectCount(any())).thenReturn(0L);
        when(modelMapper.selectById(MODEL_ID)).thenReturn(stored);
        when(providerRegistry.get("OpenAI")).thenReturn(provider);
        when(modelMapper.updateById(any(ModelEntity.class))).thenReturn(1);

        ModelCredential credential = new ModelCredential();
        credential.setApiKey(DataMaskUtil.maskApiKey(STORED_API_KEY));
        credential.setBaseUrl("https://new.example.com/v1");
        ModelEntity request = requestModel();
        request.setCredential(credential);

        ModelEntity result = service.updateModel(MODEL_ID, request);

        assertEquals(STORED_API_KEY, result.getCredential().getApiKey());
        assertEquals("https://new.example.com/v1", result.getCredential().getBaseUrl());
    }

    @Test
    void updateModel_newApiKeySubmitted_usesRequestCredential() {
        when(userContext.getUserId()).thenReturn(USER_ID);
        when(modelMapper.selectCount(any())).thenReturn(0L);
        when(modelMapper.selectById(MODEL_ID)).thenReturn(storedModel());
        when(providerRegistry.get("OpenAI")).thenReturn(provider);
        when(modelMapper.updateById(any(ModelEntity.class))).thenReturn(1);

        ModelCredential credential = new ModelCredential();
        credential.setApiKey("sk-brand-new-key");
        credential.setBaseUrl("https://new.example.com/v1");
        ModelEntity request = requestModel();
        request.setCredential(credential);

        ModelEntity result = service.updateModel(MODEL_ID, request);

        assertSame(credential, result.getCredential());
        assertEquals("sk-brand-new-key", result.getCredential().getApiKey());
    }

    @Test
    void updateModel_bothCredentialsNull_noNpe() {
        ModelEntity stored = storedModel();
        stored.setCredential(null);
        when(userContext.getUserId()).thenReturn(USER_ID);
        when(modelMapper.selectCount(any())).thenReturn(0L);
        when(modelMapper.selectById(MODEL_ID)).thenReturn(stored);
        when(providerRegistry.get("OpenAI")).thenReturn(provider);
        when(modelMapper.updateById(any(ModelEntity.class))).thenReturn(1);

        ModelEntity request = requestModel();
        request.setCredential(null);

        ModelEntity result = service.updateModel(MODEL_ID, request);

        verify(provider).modelIsValid(eq("LLM"), eq("gpt-4o"), isNull(), any());
        verify(modelMapper).updateById(same(request));
        assertSame(request, result);
    }

    @Test
    void createModel_remoteValidationRunsBeforeTransaction() {
        when(userContext.getUserId()).thenReturn(USER_ID);
        when(modelMapper.selectCount(any())).thenReturn(0L);
        when(providerRegistry.get("OpenAI")).thenReturn(provider);
        // 模拟 MyBatis-Plus 插入时回填 id
        when(modelMapper.insert(any(ModelEntity.class))).thenAnswer(inv -> {
            inv.getArgument(0, ModelEntity.class).setId(MODEL_ID);
            return 1;
        });
        when(userResourcePermissionService.ownerSave(AuthTargetType.MODEL, MODEL_ID, USER_ID)).thenReturn(true);

        ModelEntity request = requestModel();
        request.setProvider("OpenAI");

        boolean result = service.createModel(request);

        assertTrue(result);
        assertEquals(USER_ID, request.getUserId());
        // 顺序约束：远程校验 → 开事务 → 落库 → 权限映射 → 提交。
        // 校验若被移回事务内（getTransaction 先于 modelIsValid），此断言失败
        InOrder inOrder = inOrder(provider, transactionManager, modelMapper, userResourcePermissionService);
        inOrder.verify(provider).modelIsValid(eq("LLM"), eq("gpt-4o"), any(), any());
        inOrder.verify(transactionManager).getTransaction(any());
        inOrder.verify(modelMapper).insert(any(ModelEntity.class));
        inOrder.verify(userResourcePermissionService).ownerSave(AuthTargetType.MODEL, MODEL_ID, USER_ID);
        inOrder.verify(transactionManager).commit(any());
    }

    private static ModelEntity storedModel() {
        ModelEntity entity = new ModelEntity();
        entity.setId(MODEL_ID);
        entity.setName("openai-model");
        entity.setProvider("OpenAI");
        entity.setModelType("LLM");
        entity.setModelName("gpt-4o");
        entity.setUserId(USER_ID);
        ModelCredential credential = new ModelCredential();
        credential.setApiKey(STORED_API_KEY);
        credential.setBaseUrl("https://api.openai.com/v1");
        entity.setCredential(credential);
        return entity;
    }

    private static ModelEntity requestModel() {
        ModelEntity entity = new ModelEntity();
        entity.setName("openai-model-renamed");
        entity.setModelType("LLM");
        entity.setModelName("gpt-4o");
        return entity;
    }
}
