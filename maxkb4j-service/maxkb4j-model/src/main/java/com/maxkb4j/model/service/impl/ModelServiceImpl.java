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
import com.maxkb4j.common.mp.entity.ModelCredential;
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
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * @author tarzan
 * @date 2024-12-25 12:22:22
 */
@Service
@RequiredArgsConstructor
public class ModelServiceImpl extends ServiceImpl<ModelMapper, ModelEntity> implements IModelInternalService {

    private final IUserResourcePermissionService userResourcePermissionService;
    private final UserContext userContext;
    private final DataPermissionSupport dataPermissionSupport;
    private final ModelProviderRegistry providerRegistry;

    private static final Cache<String, ModelEntity> MODEL_CACHE = Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(10000)
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .build();

    @Override
    public List<ModelVO> models(ModelQuery query) {
        return baseMapper.models(query);
    }

    @Override
    public List<ModelListVO> modelList(ModelQuery query) {
        return baseMapper.modelList(query);
    }




    @Transactional
    public boolean createModel(ModelEntity model) {
        String userId = userContext.getUserId();
        if (checkModelExists(model.getName(),model.getProvider(),userId)) {
            throw new ApiException("model.name.exists");
        }
        if (model.getModelParamsForm() == null){
            model.setModelParamsForm(new JSONArray());
        }
        AbsModelProvider  modelProvider= providerRegistry.get(model.getProvider());
        JSONObject params = extractDefaultModelParams(model.getModelParamsForm());
        modelProvider.modelIsValid(model.getModelType(),model.getModelName(),model.getCredential(),params);
        model.setUserId(userId);
        model.setMeta(new JSONObject());
        model.setStatus(ModelStatus.SUCCESS.getKey());
        save(model);
        return userResourcePermissionService.ownerSave(AuthTargetType.MODEL, model.getId(), model.getUserId());
    }


    private JSONObject extractDefaultModelParams(JSONArray modelParamsForm) {
        JSONObject defaultModelParams = new JSONObject();
        // 闃插尽鎬у垽鏂細濡傛灉浼犲叆鐨勬暟缁勪负绌烘垨 null锛岀洿鎺ヨ繑鍥炵┖瀵硅薄
        if (modelParamsForm == null || modelParamsForm.isEmpty()) {
            return defaultModelParams;
        }
        // 閬嶅巻 JSONArray 涓殑姣忎竴涓厤缃」
        for (int i = 0; i < modelParamsForm.size(); i++) {
            JSONObject paramConfig = modelParamsForm.getJSONObject(i);
            // 鑾峰彇瀛楁鍚嶄綔涓?key锛岄粯璁ゅ€间綔涓?value
            String field = paramConfig.getString("field");
            Object defaultValue = paramConfig.get("default_value");
            // 闃叉 field 涓?null 瀵艰嚧寮傚父
            if (field != null && defaultValue != null) {
                defaultModelParams.put(field, defaultValue);
            }
        }
        return defaultModelParams;
    }

    public ModelEntity updateModel(String id, ModelEntity model) {
        String userId = userContext.getUserId();
        if (checkModelExists(model.getName(),model.getProvider(),userId)) {
            throw new ApiException("model.name.exists");
        }
        model.setId(id);
        ModelEntity entity = this.getById(id);
        if (entity == null) {
            throw new ApiException("model.name.not.found");
        }
        ModelCredential credential = entity.getCredential();
        String maskApiKey = DataMaskUtil.maskApiKey(credential.getApiKey());
        if (maskApiKey != null && maskApiKey.equals(model.getCredential().getApiKey())) {
            credential.setBaseUrl(model.getCredential().getBaseUrl());
            model.setCredential(credential);
        }
        AbsModelProvider  modelProvider= providerRegistry.get(entity.getProvider());
        JSONObject params = extractDefaultModelParams(entity.getModelParamsForm());
        modelProvider.modelIsValid(model.getModelType(),model.getModelName(),model.getCredential(),params);
        this.updateById(model);
        evictCache(id);
        return model;
    }

    @Transactional
    public Boolean removeModelById(String id) {
        userResourcePermissionService.remove(AuthTargetType.MODEL, id);
        evictCache(id);
        return this.removeById(id);
    }

    public ModelEntity getInfo(String id) {
        return getOwnedModel(id, model -> {
            ModelCredential credential = model.getCredential();
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
     * 搴旂敤閫氱敤绛涢€夋潯浠讹細鍚嶇О妯＄硦銆佹ā鍨嬬被鍨嬨€佷緵搴斿晢銆?
     */
    private void applyCommonFilters(LambdaQueryWrapper<ModelEntity> wrapper, String name, String modelType, String provider) {
        if (StringUtils.isNotBlank(name)) {
            wrapper.like(ModelEntity::getName, name);
        }
        if (StringUtils.isNotBlank(modelType)) {
            wrapper.eq(ModelEntity::getModelType, modelType);
        }
        if (StringUtils.isNotBlank(provider)) {
            wrapper.eq(ModelEntity::getProvider, provider);
        }
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
     * 浠?褰撳墠鐧诲綍鑰呭繀椤绘槸妯″瀷鎷ユ湁鑰?涓哄墠鎻愬彇鍑烘ā鍨嬶紝鍐嶇敱 mapper 鍐冲畾杩斿洖鍐呭銆?
     * 闈炴嫢鏈夎€呮垨妯″瀷涓嶅瓨鍦ㄦ椂杩斿洖 null銆?
     */
    private <T> T getOwnedModel(String id, Function<ModelEntity, T> mapper) {
        ModelEntity model = this.getById(id);
        if (model == null) {
            return null;
        }
        String userId = userContext.getUserId();
        if (!model.getUserId().equals(userId)) {
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
            throw new ApiException("model.name.not.found");
        }
        AbsModelProvider  modelProvider= providerRegistry.get(entity.getProvider());
        JSONObject params = extractDefaultModelParams(paramsForm);
        modelProvider.modelIsValid(entity.getModelType(),entity.getModelName(),entity.getCredential(),params);
        ModelEntity modelEntity= new ModelEntity();
        modelEntity.setId(id);
        modelEntity.setModelParamsForm(paramsForm);
        this.updateById(modelEntity);
    }

    @Override
    public String getLastModelId(ModelType modelType) {
        LambdaQueryWrapper<ModelEntity> wrapper= new LambdaQueryWrapper<>();
        wrapper.eq(ModelEntity::getModelType, modelType.getKey());
        applyDataPermission(wrapper);
        wrapper.orderByDesc(ModelEntity::getCreateTime);
        wrapper.last("limit 1");
        ModelEntity embeddingModel = this.getOne(wrapper);
        return embeddingModel != null ? embeddingModel.getId() : null;
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


    private boolean checkModelExists(String name,String provider, String userId) {
        long count = this.lambdaQuery().eq(ModelEntity::getName, name).eq(ModelEntity::getProvider, provider).eq(ModelEntity::getUserId, userId).count();
        return count > 0;
    }
}

