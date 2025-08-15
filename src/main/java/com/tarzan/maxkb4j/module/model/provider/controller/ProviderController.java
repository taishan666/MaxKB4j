package com.tarzan.maxkb4j.module.model.provider.controller;

import com.tarzan.maxkb4j.constant.AppConst;
import com.tarzan.maxkb4j.core.api.R;
import com.tarzan.maxkb4j.core.form.BaseFiled;
import com.tarzan.maxkb4j.module.model.info.vo.KeyAndValueVO;
import com.tarzan.maxkb4j.module.model.provider.IModelProvider;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelProviderEnum;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelTypeEnum;
import com.tarzan.maxkb4j.module.model.provider.impl.aliyunModelProvider.AliYunBaiLianModelProvider;
import com.tarzan.maxkb4j.module.model.provider.impl.azuremodelprovider.AzureModelProvider;
import com.tarzan.maxkb4j.module.model.provider.impl.deepseekmodelprovider.DeepSeekModelProvider;
import com.tarzan.maxkb4j.module.model.provider.impl.kimimodelprovider.KimiModelProvider;
import com.tarzan.maxkb4j.module.model.provider.impl.ollamamodelprovider.OLlamaModelProvider;
import com.tarzan.maxkb4j.module.model.provider.impl.openaimodelprovider.OpenaiModelProvider;
import com.tarzan.maxkb4j.module.model.provider.impl.volcanicenginemodelprovider.VolcanicEngineModelProvider;
import com.tarzan.maxkb4j.module.model.provider.impl.xinferencemodelprovider.XInferenceModelProvider;
import com.tarzan.maxkb4j.module.model.provider.impl.zhipumodelprovider.ZhiPuModelProvider;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelInfo;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelProvideInfo;
import com.tarzan.maxkb4j.util.StringUtil;
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
@RequestMapping(AppConst.ADMIN_PATH)
@AllArgsConstructor
public class ProviderController {

	//@SaCheckPermission("MODEL:READ")
    @GetMapping("/provider")
	public R<Set<ModelProvideInfo>> provider(String modelType){
		Set<IModelProvider> set= new HashSet<>();
		set.add(new AliYunBaiLianModelProvider());
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
			return R.success(set.stream().map(IModelProvider::getModelProvideInfo).collect(Collectors.toSet()));
		}
		return R.success(set.stream().filter(e->e.isSupport(modelType)).map(IModelProvider::getModelProvideInfo).collect(Collectors.toSet()));
	}


	//@SaCheckPermission("MODEL:READ")
	@GetMapping("/provider/model_type_list")
	public R<List<KeyAndValueVO>> modelTypeList(String provider){
		IModelProvider modelProvider= ModelProviderEnum.get(provider);
		List<ModelInfo> modelInfos=modelProvider.getModelList();
		List<KeyAndValueVO> list= ModelTypeEnum.getModelTypeList();
		Map<String,List<ModelInfo>> map=modelInfos.stream().collect(Collectors.groupingBy(ModelInfo::getModelType));
		Set<String> keys=map.keySet();
		list.removeIf(e -> !keys.contains(e.getValue()));
		return R.success(list);
	}

	//@SaCheckPermission("MODEL:READ")
	@GetMapping("/provider/model_form")
	public R<List<BaseFiled>> modelForm(String provider, String modelType, String modelName){
		IModelProvider modelProvider=ModelProviderEnum.get(provider);
		return R.success(modelProvider.getModelCredential().toForm());
	}


	//@SaCheckPermission("MODEL:READ")
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
