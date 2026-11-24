package com.maxkb4j.model.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.maxkb4j.common.context.UserContext;
import com.maxkb4j.common.exception.ApiException;
import com.maxkb4j.model.entity.ModelCredential;
import com.maxkb4j.common.util.DataMaskUtil;
import com.maxkb4j.core.support.permission.DataPermissionScope;
import com.maxkb4j.core.support.permission.DataPermissionSupport;
import com.maxkb4j.model.dto.ModelQuery;
import com.maxkb4j.model.entity.ModelEntity;
import com.maxkb4j.model.enums.ModelStatus;
import com.maxkb4j.model.enums.ModelType;
import com.maxkb4j.model.mapper.ModelMapper;
import com.maxkb4j.model.provider.AbsModelProvider;
import com.maxkb4j.model.registry.ModelProviderRegistry;
import com.maxkb4j.model.service.IModelInternalService;
import com.maxkb4j.model.vo.ModelListVO;
import com.maxkb4j.model.vo.ModelVO;
import com.maxkb4j.system.constant.AuthTargetType;
import com.maxkb4j.user.service.IUserResourcePermissionService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.maxkb4j.model.consts.ModelConstants.*;

/**
 * @author tarzan
 * @date 2024-12-25 12:22:22
 */
@Service
@RequiredArgsConstructor
public class ModelServiceImpl extends ServiceImpl<ModelMapper, ModelEntity> implements IModelInternalService {

    private static final int MODEL_CACHE_INITIAL_CAPACITY = 100;

    /**
     * 模型缓存最大容量。
     */
    private static final int MODEL_CACHE_MAXIMUM_SIZE = 10000;

    /**
     * 模型缓存过期时间（分钟）。
     */
    private static final int MODEL_CACHE_EXPIRE_MINUTES = 1;
    private static final Cache<String, ModelEntity> MODEL_CACHE = Caffeine.newBuilder()
            .initialCapacity(MODEL_CACHE_INITIAL_CAPACITY)
            .maximumSize(MODEL_CACHE_MAXIMUM_SIZE)
            .expireAfterWrite(MODEL_CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES)
            .expireAfterAccess(MODEL_CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES)
            .build();
    private final IUserResourcePermissionService userResourcePermissionService;
    private final UserContext userContext;
    private final DataPermissionSupport dataPermissionSupport;
    private final ModelProviderRegistry providerRegistry;
    private final PlatformTransactionManager transactionManager;
    private TransactionTemplate transactionTemplate;

    @PostConstruct
    private void initTransactionTemplate() {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public List<ModelVO> models(ModelQuery query) {
        return baseMapper.models(query);
    }

    @Override
    public List<ModelListVO> modelList(ModelQuery query) {
        return baseMapper.modelList(query);
    }


    /**
     * 注意：本方法故意不加 @Transactional——方法级事务会在入口即占用数据库连接，
     * 而 modelIsValid 的 LLM 校验是阻塞式远程调用（flux.blockLast()，读超时默认 30 分钟），
     * 若包进事务，校验挂到 read-timeout 期间会一直占住连接，并发创建几个模型即可耗尽连接池。
     * 事务只覆盖数据库写入（见 transactionTemplate），校验保持在事务之外。
     */
    public boolean createModel(ModelEntity model) {
        String userId = userContext.getUserId();
        if (checkModelExists(null, model.getName(), userId)) {
            throw new ApiException(MessageCode.MODEL_NAME_EXISTS);
        }
        if (model.getModelParamsForm() == null) {
            model.setModelParamsForm(new JSONArray());
        }
        // 远程校验在事务外执行，避免长校验期间占死数据库连接
        AbsModelProvider modelProvider = providerRegistry.get(model.getProvider());
        if (modelProvider == null) {
            throw new ApiException("model.provider.not.exists", model.getProvider());
        }
        JSONObject params = extractDefaultModelParams(model.getModelParamsForm());
        modelProvider.modelIsValid(model.getModelType(), model.getModelName(), model.getCredential(), params);
        model.setUserId(userId);
        model.setMeta(new JSONObject());
        model.setStatus(ModelStatus.SUCCESS.getKey());
        // 模型与权限映射同生共死：任一写入失败整体回滚
        return Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            save(model);
            return userResourcePermissionService.ownerSave(AuthTargetType.MODEL, model.getId(), model.getUserId());
        }));
    }


    private JSONObject extractDefaultModelParams(JSONArray modelParamsForm) {
        JSONObject defaultModelParams = new JSONObject();
        if (modelParamsForm == null || modelParamsForm.isEmpty()) {
            return defaultModelParams;
        }
        for (int i = 0; i < modelParamsForm.size(); i++) {
            JSONObject paramConfig = modelParamsForm.getJSONObject(i);
            String field = paramConfig.getString(ParamKey.FIELD);
            Object defaultValue = paramConfig.get(ParamKey.DEFAULT_VALUE);
            if (field != null && defaultValue != null) {
                defaultModelParams.put(field, defaultValue);
            }
        }
        return defaultModelParams;
    }

    public ModelEntity updateModel(String id, ModelEntity model) {
        String userId = userContext.getUserId();
        if (checkModelExists(id, model.getName(), userId)) {
            throw new ApiException(MessageCode.MODEL_NAME_EXISTS);
        }
        model.setId(id);
        ModelEntity entity = this.getById(id);
        if (entity == null) {
            throw new ApiException(MessageCode.MODEL_NAME_NOT_FOUND);
        }
        ModelCredential entityCredential = entity.getCredential();
        ModelCredential requestCredential = model.getCredential();
        if (requestCredential == null) {
            // 未传 credential：沿用库中原凭证，避免空指针，也保证后续校验拿得到密钥
            model.setCredential(entityCredential);
        } else if (entityCredential != null) {
            String maskApiKey = DataMaskUtil.maskApiKey(entityCredential.getApiKey());
            if (maskApiKey != null && maskApiKey.equals(requestCredential.getApiKey())) {
                // 回传的是脱敏后的 apiKey：保留原密钥，仅更新 baseUrl
                entityCredential.setBaseUrl(requestCredential.getBaseUrl());
                model.setCredential(entityCredential);
            }
        }
        AbsModelProvider modelProvider = providerRegistry.get(entity.getProvider());
        if (modelProvider == null) {
            throw new ApiException("model.provider.not.exists", entity.getProvider());
        }
        JSONObject params = extractDefaultModelParams(entity.getModelParamsForm());
        modelProvider.modelIsValid(model.getModelType(), model.getModelName(), model.getCredential(), params);
        this.updateById(model);
        evictCache(id);
        return model;
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean removeModelById(String id) {
        userResourcePermissionService.remove(AuthTargetType.MODEL, id);
        evictCache(id);
        return this.removeById(id);
    }

    public ModelEntity getInfo(String id) {
        return getOwnedModel(id, model -> {
            ModelCredential credential = model.getCredential();
            // 存量/导入数据 credential 列可能为 NULL，无密钥可脱敏，直接原样返回
            if (credential == null) {
                return model;
            }
            credential.setApiKey(DataMaskUtil.maskApiKey(credential.getApiKey()));
            return model;
        });
    }

    public ModelEntity getModelById(String id) {
        if (StringUtils.isBlank(id)) {
            return null;
        }
        return MODEL_CACHE.get(id, modelId -> this.lambdaQuery()
                .select(ModelEntity::getProvider, ModelEntity::getModelType, ModelEntity::getModelName, ModelEntity::getCredential)
                .eq(ModelEntity::getId, modelId)
                .one());
    }

    /**
     * 应用数据权限：
     * - 管理员：不附加限制；
     * - 普通用户：仅可见已授权的模型，无授权则强制空结果；
     * - 无角色：强制空结果。
     */
    private void applyDataPermission(LambdaQueryWrapper<ModelEntity> wrapper) {
        DataPermissionScope scope = dataPermissionSupport.resolve(AuthTargetType.MODEL);
        if (scope.isEmptyResult()) {
            wrapper.last(" limit 0");
            return;
        }
        if (!scope.isAdmin()) {
            wrapper.in(ModelEntity::getId, scope.getTargetIds());
        }
    }

    /**
     * 以当前登录者必须是模型拥有者为前提取出模型，再由 mapper 决定返回内容。
     * 非拥有者或模型不存在时返回 null。
     */
    private <T> T getOwnedModel(String id, Function<ModelEntity, T> mapper) {
        ModelEntity model = this.getById(id);
        if (model == null) {
            return null;
        }
        String userId = userContext.getUserId();
        // 存量数据 user_id 可能为 NULL，用 Objects.equals 避免拆箱/空指针
        if (!Objects.equals(model.getUserId(), userId)) {
            return null;
        }
        return mapper.apply(model);
    }

    private void evictCache(String id) {
        MODEL_CACHE.invalidate(id);
    }

    public void updateModelParamsForm(String id, JSONArray paramsForm) {
        ModelEntity entity = this.getById(id);
        if (entity == null) {
            throw new ApiException(MessageCode.MODEL_NAME_NOT_FOUND);
        }
        AbsModelProvider modelProvider = providerRegistry.get(entity.getProvider());
        JSONObject params = extractDefaultModelParams(paramsForm);
        modelProvider.modelIsValid(entity.getModelType(), entity.getModelName(), entity.getCredential(), params);
        ModelEntity modelEntity = new ModelEntity();
        modelEntity.setId(id);
        modelEntity.setModelParamsForm(paramsForm);
        this.updateById(modelEntity);
        // 当前缓存投影不含 modelParamsForm，但失效与 updateModel 保持一致，避免投影扩列后读到旧值
        evictCache(id);
    }

    private String getLastModelId(ModelType modelType) {
        LambdaQueryWrapper<ModelEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModelEntity::getModelType, modelType.getKey());
        applyDataPermission(wrapper);
        wrapper.orderByDesc(ModelEntity::getCreateTime);
        wrapper.last("limit 1");
        ModelEntity model = this.getOne(wrapper);
        return model != null ? model.getId() : null;
    }

    @Override
    public String getSafeModelId(String modelId, ModelType modelType) {
        if (StringUtils.isNotBlank(modelId)) {
            LambdaQueryWrapper<ModelEntity> wrapper = Wrappers.lambdaQuery();
            wrapper.select(ModelEntity::getId);
            wrapper.eq(ModelEntity::getId, modelId);
            applyDataPermission(wrapper);
            if (this.getOne(wrapper) != null) {
                return modelId;
            }
        }
        return getLastModelId(modelType);
    }


    private boolean checkModelExists(String id, String name, String userId) {
        long count;
        if (StringUtils.isBlank(id)) {
            count = this.lambdaQuery().eq(ModelEntity::getName, name).eq(ModelEntity::getUserId, userId).count();
        } else {
            count = this.lambdaQuery().eq(ModelEntity::getName, name).eq(ModelEntity::getUserId, userId).ne(ModelEntity::getId, id).count();
        }
        return count > 0;
    }
}

