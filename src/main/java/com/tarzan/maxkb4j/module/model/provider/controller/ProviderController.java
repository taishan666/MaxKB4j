package com.tarzan.maxkb4j.module.model.provider.controller;

import com.tarzan.maxkb4j.common.api.R;
import com.tarzan.maxkb4j.common.constant.AppConst;
import com.tarzan.maxkb4j.common.form.BaseFiled;
import com.tarzan.maxkb4j.common.util.StringUtil;
import com.tarzan.maxkb4j.module.model.info.vo.KeyAndValueVO;
import com.tarzan.maxkb4j.module.model.provider.service.IModelProvider;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelProviderEnum;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelType;
import com.tarzan.maxkb4j.module.model.provider.service.impl.aliyunModelProvider.AliYunBaiLianModelProvider;
import com.tarzan.maxkb4j.module.model.provider.service.impl.azuremodelprovider.AzureModelProvider;
import com.tarzan.maxkb4j.module.model.provider.service.impl.deepseekmodelprovider.DeepSeekModelProvider;
import com.tarzan.maxkb4j.module.model.provider.service.impl.kimimodelprovider.KimiModelProvider;
import com.tarzan.maxkb4j.module.model.provider.service.impl.localmodelprovider.LocalModelProvider;
import com.tarzan.maxkb4j.module.model.provider.service.impl.ollamamodelprovider.OLlamaModelProvider;
import com.tarzan.maxkb4j.module.model.provider.service.impl.openaimodelprovider.OpenaiModelProvider;
import com.tarzan.maxkb4j.module.model.provider.service.impl.volcanicenginemodelprovider.VolcanicEngineModelProvider;
import com.tarzan.maxkb4j.module.model.provider.service.impl.xinferencemodelprovider.XInferenceModelProvider;
import com.tarzan.maxkb4j.module.model.provider.service.impl.zhipumodelprovider.ZhiPuModelProvider;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelInfo;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelProviderInfo;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
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
	public R<Set<ModelProviderInfo>> provider(String modelType){
		Set<IModelProvider> set= new HashSet<>();
		set.add(new AliYunBaiLianModelProvider());
		set.add(new LocalModelProvider());
		//set.add(new AwsBedrockModelProvider());
		set.add(new AzureModelProvider());
		set.add(new DeepSeekModelProvider());
		set.add(new KimiModelProvider());
		set.add(new OLlamaModelProvider());
		set.add(new OpenaiModelProvider());
		//set.add(new GeminiModelProvider());
	//	set.add(new QwenModelProvider());
	//	set.add(new TencentModelProvider());
	//	set.add(new WenXinModelProvider());
		//set.add(new XfModelProvider());
		set.add(new ZhiPuModelProvider());
	//	set.add(new VLlmModelProvider());
		set.add(new XInferenceModelProvider());
		set.add(new VolcanicEngineModelProvider());
	//	set.add(new LocalModelProvider());
		if (StringUtil.isBlank(modelType)){
			return R.success(set.stream().map(IModelProvider::getBaseInfo).collect(Collectors.toSet()));
		}
		return R.success(set.stream().filter(e->e.isSupport(modelType)).map(IModelProvider::getBaseInfo).collect(Collectors.toSet()));
	}


	@GetMapping("/provider/model_type_list")
	public R<List<KeyAndValueVO>> modelTypeList(String provider){
		IModelProvider modelProvider= ModelProviderEnum.get(provider);
		List<ModelInfo> modelInfos=modelProvider.getModelList();
		List<KeyAndValueVO> list= ModelType.getModelTypeList();
		Map<String,List<ModelInfo>> map=modelInfos.stream().collect(Collectors.groupingBy(ModelInfo::getModelType));
		Set<String> keys=map.keySet();
		list.removeIf(e -> !keys.contains(e.getValue()));
		return R.success(list);
	}

	@GetMapping("/provider/model_form")
	public R<List<BaseFiled>> modelForm(String provider, String modelType, String modelName){
		IModelProvider modelProvider=ModelProviderEnum.get(provider);
		return R.success(modelProvider.getModelCredential().toForm());
	}


	@GetMapping("/provider/model_params_form")
	public R<List<BaseFiled>> modelParamsForm(String provider, String modelType, String modelName){
		IModelProvider modelProvider=ModelProviderEnum.get(provider);
		return R.success(modelProvider.getModelParams(modelType, modelName).toForm());
	}


	@GetMapping("/provider/model_list")
	public R<List<ModelInfo>> modelList(String provider, String modelType){
		IModelProvider modelProvider=ModelProviderEnum.get(provider);
		List<ModelInfo> modelInfos=modelProvider.getModelList();
		if (StringUtil.isBlank(modelType)){
			return R.success(modelInfos);
		}
		List<ModelInfo>  modelList=modelInfos.stream().filter(e->e.getModelType().equals(modelType)).toList();
		return R.success(modelList);
	}


}
