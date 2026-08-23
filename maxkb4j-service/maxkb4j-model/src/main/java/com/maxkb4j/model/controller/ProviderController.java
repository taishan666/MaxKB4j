package com.maxkb4j.model.controller;

import com.maxkb4j.common.api.R;
import com.maxkb4j.common.constant.AppConst;
import com.maxkb4j.common.domain.dto.KeyAndValue;
import com.maxkb4j.model.form.BaseField;
import com.maxkb4j.model.enums.ModelType;
import com.maxkb4j.model.provider.AbsModelProvider;
import com.maxkb4j.model.registry.ModelProviderRegistry;
import com.maxkb4j.model.vo.ModelInfo;
import com.maxkb4j.model.vo.ModelProviderInfo;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import static com.maxkb4j.model.consts.ModelConstants.*;

/**
 * Model Provider Controller
 * Provides APIs for querying provider and model information
 */
@RestController
@RequestMapping(AppConst.ADMIN_API)
@RequiredArgsConstructor
public class ProviderController {

    private final ModelProviderRegistry providerRegistry;

    @GetMapping(ApiPath.PROVIDER)
    public R<List<ModelProviderInfo>> provider(String modelType) {
        if (StringUtils.isBlank(modelType)) {
            return R.data(providerRegistry.getProviderInfos());
        }
        List<ModelProviderInfo> list = providerRegistry.all().stream()
                .filter(rp -> rp.provider().isSupport(ModelType.getByKey(modelType)))
                .map(rp -> new ModelProviderInfo(rp.key(), rp.name(), rp.icon()))
                .toList();
        return R.data(list);
    }


    @GetMapping(ApiPath.PROVIDER_MODEL_TYPE_LIST)
    public R<List<KeyAndValue>> modelTypeList(String provider) {
        AbsModelProvider modelProvider = providerRegistry.get(provider);
        if (modelProvider == null) {
            return R.data(List.of());
        }
        List<ModelInfo> modelInfos = modelProvider.getModelList();
        Map<ModelType, List<ModelInfo>> map = modelInfos.stream().collect(Collectors.groupingBy(ModelInfo::getModelType));
        Set<ModelType> keys = map.keySet();
        List<KeyAndValue> list = ModelType.getModelTypeList().stream().filter(keys::contains).map(e -> new KeyAndValue(e.getName(), e.getKey())).toList();
        return R.data(list);
    }

    @GetMapping(ApiPath.PROVIDER_MODEL_FORM)
    public R<List<BaseField>> modelForm(String provider, String modelType, String modelName) {
        AbsModelProvider modelProvider = providerRegistry.get(provider);
        if (modelProvider == null) {
            return R.data(List.of());
        }
        return R.data(modelProvider.getModelCredential().toForm());
    }


    @GetMapping(ApiPath.PROVIDER_MODEL_PARAMS_FORM)
    public R<List<BaseField>> modelParamsForm(String provider, String modelType, String modelName) {
        AbsModelProvider modelProvider = providerRegistry.get(provider);
        if (modelProvider == null){
            return R.data(List.of());
        }
        ModelInfo modelInfo = modelProvider.getModelInfo(ModelType.getByKey(modelType), modelName);
        if (modelInfo == null || modelInfo.getModelParams() == null) {
            return R.data(modelProvider.getModelParamsForm(modelType));
        }
        return R.data(modelInfo.getModelParams().toForm());
    }


    @GetMapping(ApiPath.PROVIDER_MODEL_LIST)
    public R<List<ModelInfo>> modelList(String provider, String modelType) {
        AbsModelProvider modelProvider = providerRegistry.get(provider);
        if (modelProvider == null) {
            return R.data(List.of());
        }
        List<ModelInfo> modelInfos = modelProvider.getModelList();
        if (StringUtils.isBlank(modelType)) {
            return R.data(modelInfos);
        }
        List<ModelInfo> modelList = modelInfos.stream().filter(e -> e.getModelType().getKey().equals(modelType)).toList();
        return R.data(modelList);
    }


}