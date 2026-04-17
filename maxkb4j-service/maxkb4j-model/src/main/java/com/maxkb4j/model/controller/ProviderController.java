package com.maxkb4j.model.controller;

import com.maxkb4j.common.constant.AppConst;
import com.maxkb4j.common.api.R;
import com.maxkb4j.common.domain.form.BaseField;
import com.maxkb4j.common.domain.vo.KeyAndValueVO;
import com.maxkb4j.model.enums.ModelProvider;
import com.maxkb4j.model.enums.ModelType;
import com.maxkb4j.model.provider.AbsModelProvider;
import com.maxkb4j.model.vo.ModelInfo;
import com.maxkb4j.model.vo.ModelProviderInfo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Model Provider Controller
 * Provides APIs for querying provider and model information
 */
@RestController
@RequestMapping(AppConst.ADMIN_API)
public class ProviderController {

    @GetMapping("/provider")
    public R<List<ModelProviderInfo>> provider(String modelType) {
        ModelProvider[] providerEnums = ModelProvider.values();
        if (StringUtils.isBlank(modelType)) {
            return R.success(Arrays.stream(providerEnums).map(ModelProvider::getInfo).toList());
        }
        List<ModelProviderInfo> list = Arrays.stream(providerEnums).filter(e -> {
            AbsModelProvider modelProvider = e.getModelProvider();
            return modelProvider.isSupport(ModelType.getByKey(modelType));
        }).map(ModelProvider::getInfo).toList();
        return R.success(list);
    }


    @GetMapping("/provider/model_type_list")
    public R<List<KeyAndValueVO>> modelTypeList(String provider) {
        AbsModelProvider modelProvider = ModelProvider.get(provider);
        List<ModelInfo> modelInfos = modelProvider.getModelList();
        Map<ModelType, List<ModelInfo>> map = modelInfos.stream().collect(Collectors.groupingBy(ModelInfo::getModelType));
        Set<ModelType> keys = map.keySet();
        List<KeyAndValueVO> list = ModelType.getModelTypeList().stream().filter(keys::contains).map(e -> new KeyAndValueVO(e.getName(), e.getKey())).toList();
        return R.success(list);
    }

    @GetMapping("/provider/model_form")
    public R<List<BaseField>> modelForm(String provider, String modelType, String modelName) {
        AbsModelProvider modelProvider = ModelProvider.get(provider);
        return R.success(modelProvider.getModelCredential().toForm());
    }


    @GetMapping("/provider/model_params_form")
    public R<List<BaseField>> modelParamsForm(String provider, String modelType, String modelName) {
        AbsModelProvider modelProvider = ModelProvider.get(provider);
        if (modelProvider == null){
            return R.success(List.of());
        }
        ModelInfo modelInfo = modelProvider.getModelInfo(ModelType.getByKey(modelType), modelName);
        if (modelInfo == null || modelInfo.getModelParams() == null) {
            return R.success(modelProvider.getModelParamsForm(modelType));
        }
        return R.success(modelInfo.getModelParams().toForm());
    }


    @GetMapping("/provider/model_list")
    public R<List<ModelInfo>> modelList(String provider, String modelType) {
        AbsModelProvider modelProvider = ModelProvider.get(provider);
        List<ModelInfo> modelInfos = modelProvider.getModelList();
        if (StringUtils.isBlank(modelType)) {
            return R.success(modelInfos);
        }
        List<ModelInfo> modelList = modelInfos.stream().filter(e -> e.getModelType().getKey().equals(modelType)).toList();
        return R.success(modelList);
    }


}
