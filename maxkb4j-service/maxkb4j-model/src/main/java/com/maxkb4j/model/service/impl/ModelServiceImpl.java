package com.maxkb4j.model.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.maxkb4j.common.constant.RoleType;
import com.maxkb4j.common.context.UserContext;
import com.maxkb4j.common.exception.ApiException;
import com.maxkb4j.common.mp.entity.ModelCredential;
import com.maxkb4j.common.util.BeanUtil;
import com.maxkb4j.common.util.DataMaskUtil;
import com.maxkb4j.model.entity.ModelEntity;
import com.maxkb4j.model.registry.ModelProviderRegistry;
import com.maxkb4j.model.enums.ModelStatus;
import com.maxkb4j.model.mapper.ModelMapper;
import com.maxkb4j.model.provider.AbsModelProvider;
import com.maxkb4j.model.service.IModelInternalService;
import com.maxkb4j.model.vo.ModelVO;
import com.maxkb4j.system.constant.AuthTargetType;
import com.maxkb4j.user.service.IUserResourcePermissionService;
import com.maxkb4j.user.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * @author tarzan
 * @date 2024-12-25 12:22:22
 */
@Service
@RequiredArgsConstructor
public class ModelServiceImpl extends ServiceImpl<ModelMapper, ModelEntity> implements IModelInternalService {

    private final IUserService userService;
    private final IUserResourcePermissionService userResourcePermissionService;
    private final UserContext userContext;
    private final ModelProviderRegistry providerRegistry;

    private static final Cache<String, ModelEntity> MODEL_CACHE = Caffeine.newBuilder()
            .initialCapacity(100)
            // 瓒呭嚭鏈€澶у閲忔椂娣樻卑
            .maximumSize(10000)
            //璁剧疆鍐欑紦瀛樺悗n绉掗挓杩囨湡
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .build();

    public List<ModelVO> modelList(String name, String modelName, String modelType, String provider) {
        LambdaQueryWrapper<ModelEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.select(ModelEntity::getId,
                ModelEntity::getName,
                ModelEntity::getModelName,
                ModelEntity::getProvider,
                ModelEntity::getModelParamsForm,
                ModelEntity::getStatus
        );
        applyCommonFilters(wrapper, name, modelType, provider);
        if (StringUtils.isNotBlank(modelName)) {
            wrapper.eq(ModelEntity::getModelName, modelName);
        }
        applyDataPermission(wrapper);
        wrapper.orderByDesc(ModelEntity::getCreateTime);
        List<ModelEntity> modelEntities = this.list(wrapper);
        if (CollectionUtils.isNotEmpty(modelEntities)) {
            return BeanUtil.copyList(modelEntities, ModelVO.class);
        }
        return Collections.emptyList();
    }

    public List<ModelVO> models(String name, String createUserId, String modelType, String provider) {
        LambdaQueryWrapper<ModelEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.select(ModelEntity::getId,
                ModelEntity::getName,
                ModelEntity::getModelName,
                ModelEntity::getModelType,
                ModelEntity::getProvider,
                ModelEntity::getUserId,
                ModelEntity::getStatus,
                ModelEntity::getCreateTime
        );
        applyCommonFilters(wrapper, name, modelType, provider);
        if (StringUtils.isNotBlank(createUserId)) {
            wrapper.eq(ModelEntity::getUserId, createUserId);
        }
        applyDataPermission(wrapper);
        wrapper.orderByDesc(ModelEntity::getCreateTime);
        List<ModelEntity> modelEntities = this.list(wrapper);
        if (CollectionUtils.isEmpty(modelEntities)) {
            return Collections.emptyList();
        }
        Map<String, String> userMap = userService.getNicknameMap();
        List<ModelVO> models = BeanUtil.copyList(modelEntities, ModelVO.class);
        models.forEach(model -> model.setNickname(userMap.get(model.getUserId())));
        return models;
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
     * 搴旂敤鏁版嵁鏉冮檺锛?
     * - 鏅€氱敤鎴凤細浠呭彲瑙佸凡鎺堟潈鐨勬ā鍨嬶紝鏃犳巿鏉冨垯寮哄埗绌虹粨鏋?
     * - 鏃犱换浣曡鑹诧細寮哄埗绌虹粨鏋?
     * - 鍏朵粬瑙掕壊锛堝绠＄悊鍛橈級锛氫笉闄勫姞闄愬埗
     */
    private void applyDataPermission(LambdaQueryWrapper<ModelEntity> wrapper) {
        String loginId = userContext.getUserId();
        Set<String> roles = userService.getRoleById(loginId);
        if (CollectionUtils.isEmpty(roles)) {
            wrapper.last(" limit 0");
            return;
        }
        if (!roles.contains(RoleType.USER)) {
            return;
        }
        List<String> targetIds = userResourcePermissionService.getTargetIds(AuthTargetType.MODEL, loginId);
        if (org.springframework.util.CollectionUtils.isEmpty(targetIds)) {
            wrapper.last(" limit 0");
        } else {
            wrapper.in(ModelEntity::getId, targetIds);
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
    public String getLastModelId(String modelType,String userId) {
        ModelEntity embeddingModel = this.lambdaQuery()
                .eq(ModelEntity::getModelType, modelType)
                .eq(ModelEntity::getUserId, userId)
                .orderByDesc(ModelEntity::getCreateTime)
                .last("limit 1")
                .one();
        return embeddingModel != null ? embeddingModel.getId() : null;
    }


    private boolean checkModelExists(String name,String provider, String userId) {
        long count = this.lambdaQuery().eq(ModelEntity::getName, name).eq(ModelEntity::getProvider, provider).eq(ModelEntity::getUserId, userId).count();
        return count > 0;
    }
}

