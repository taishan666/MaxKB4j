package com.tarzan.maxkb4j.module.model.provider.controller;

import com.tarzan.maxkb4j.common.api.R;
import com.tarzan.maxkb4j.common.constant.AppConst;
import com.tarzan.maxkb4j.common.form.BaseField;
import com.tarzan.maxkb4j.module.model.info.vo.KeyAndValueVO;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelProviderEnum;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelType;
import com.tarzan.maxkb4j.module.model.provider.service.IModelProvider;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelInfo;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelProviderInfo;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author tarzan
 * @date 2024-12-25 12:22:22
 */
@RestController
@RequestMapping(AppConst.ADMIN_API)
@AllArgsConstructor
public class ProviderController {

    @GetMapping("/provider")
    public R<List<ModelProviderInfo>> provider(String modelType) {
        List<IModelProvider> list = ModelProviderEnum.getAllProvider();
        if (StringUtils.isBlank(modelType)) {
            return R.success(list.stream().map(IModelProvider::getBaseInfo).toList());
        }
        return R.success(list.stream().filter(e -> e.isSupport(ModelType.getByKey(modelType))).map(IModelProvider::getBaseInfo).toList());
    }


    @GetMapping("/provider/model_type_list")
    public R<List<KeyAndValueVO>> modelTypeList(String provider) {
        IModelProvider modelProvider = ModelProviderEnum.get(provider);
        List<ModelInfo> modelInfos = modelProvider.getModelList();
        Map<ModelType, List<ModelInfo>> map = modelInfos.stream().collect(Collectors.groupingBy(ModelInfo::getModelType));
        Set<ModelType> keys = map.keySet();
        List<KeyAndValueVO> list = ModelType.getModelTypeList().stream().filter(keys::contains).map(e -> new KeyAndValueVO(e.getName(), e.getKey())).toList();
        return R.success(list);
    }

    @GetMapping("/provider/model_form")
    public R<List<BaseField>> modelForm(String provider, String modelType, String modelName) {
        IModelProvider modelProvider = ModelProviderEnum.get(provider);
        return R.success(modelProvider.getModelCredential().toForm());
    }


    @GetMapping("/provider/model_params_form")
    public R<List<BaseField>> modelParamsForm(String provider, String modelType, String modelName) {
        IModelProvider modelProvider = ModelProviderEnum.get(provider);
        ModelInfo modelInfo = modelProvider.getModelInfo(ModelType.getByKey(modelType), modelName);
        if (modelInfo == null|| modelInfo.getModelParams()==null){
            return R.success(List.of());
        }
        return R.success(modelInfo.getModelParams().toForm());
    }


    @GetMapping("/provider/model_list")
    public R<List<ModelInfo>> modelList(String provider, String modelType) {
        IModelProvider modelProvider = ModelProviderEnum.get(provider);
        List<ModelInfo> modelInfos = modelProvider.getModelList();
        if (StringUtils.isBlank(modelType)) {
            return R.success(modelInfos);
        }
        List<ModelInfo> modelList = modelInfos.stream().filter(e -> e.getModelType().getKey().equals(modelType)).toList();
        return R.success(modelList);
    }


}
