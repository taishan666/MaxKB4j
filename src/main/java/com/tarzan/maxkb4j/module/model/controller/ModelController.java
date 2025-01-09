package com.tarzan.maxkb4j.module.model.controller;

import com.tarzan.maxkb4j.module.model.entity.ModelEntity;
import com.tarzan.maxkb4j.module.model.service.ModelService;
import com.tarzan.maxkb4j.module.modelprovider.IModelProvider;
import com.tarzan.maxkb4j.module.modelprovider.KeyAndValueVO;
import com.tarzan.maxkb4j.module.modelprovider.ModelInfo;
import com.tarzan.maxkb4j.module.modelprovider.ModelProvideInfo;
import com.tarzan.maxkb4j.module.modelprovider.aliyunModelProvider.AliYunBaiLianModelProvider;
import com.tarzan.maxkb4j.module.modelprovider.awsbedrockmodelprovider.AwsBedrockModelProvider;
import com.tarzan.maxkb4j.module.modelprovider.azuremodelprovider.AzureModelProvider;
import com.tarzan.maxkb4j.module.modelprovider.deepseekmodelprovider.DeepSeekModelProvider;
import com.tarzan.maxkb4j.module.modelprovider.geminimodelprovider.GeminiModelProvider;
import com.tarzan.maxkb4j.module.modelprovider.kimimodelprovider.KimiModelProvider;
import com.tarzan.maxkb4j.module.modelprovider.localmodelprovider.LocalModelProvider;
import com.tarzan.maxkb4j.module.modelprovider.ollamamodelprovider.OLlamaModelProvider;
import com.tarzan.maxkb4j.module.modelprovider.openaimodelprovider.OpenaiModelProvider;
import com.tarzan.maxkb4j.module.modelprovider.qwenmodelprovider.QwenModelProvider;
import com.tarzan.maxkb4j.module.modelprovider.tencentmodelprovider.TencentModelProvider;
import com.tarzan.maxkb4j.module.modelprovider.vllmmodelprovider.VLlmModelProvider;
import com.tarzan.maxkb4j.module.modelprovider.volcanicenginemodelprovider.VolcanicEngineModelProvider;
import com.tarzan.maxkb4j.module.modelprovider.wenxinmodelprovider.WenXinModelProvider;
import com.tarzan.maxkb4j.module.modelprovider.xfmodelprovider.XfModelProvider;
import com.tarzan.maxkb4j.module.modelprovider.xinferencemodelprovider.XInferenceModelProvider;
import com.tarzan.maxkb4j.module.modelprovider.zhipumodelprovider.ZhiPuModelProvider;
import com.tarzan.maxkb4j.tool.api.R;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author tarzan
 * @date 2024-12-25 12:22:22
 */
@RestController
@AllArgsConstructor
public class ModelController{

	@Autowired
	private ModelService modelService;
	@Autowired
	private Map<String, IModelProvider> modelProviderMap;

    @GetMapping("api/provider")
	public R<Set<ModelProvideInfo>> provider(){
		Set<ModelProvideInfo> set= new HashSet<>();
		set.add(new AliYunBaiLianModelProvider().getModelProvideInfo());
		set.add(new AwsBedrockModelProvider().getModelProvideInfo());
		set.add(new AzureModelProvider().getModelProvideInfo());
		set.add(new DeepSeekModelProvider().getModelProvideInfo());
		set.add(new KimiModelProvider().getModelProvideInfo());
		set.add(new OLlamaModelProvider().getModelProvideInfo());
		set.add(new OpenaiModelProvider().getModelProvideInfo());
		set.add(new GeminiModelProvider().getModelProvideInfo());
		set.add(new QwenModelProvider().getModelProvideInfo());
		set.add(new TencentModelProvider().getModelProvideInfo());
		set.add(new WenXinModelProvider().getModelProvideInfo());
		set.add(new XfModelProvider().getModelProvideInfo());
		set.add(new ZhiPuModelProvider().getModelProvideInfo());
		set.add(new VLlmModelProvider().getModelProvideInfo());
		set.add(new XInferenceModelProvider().getModelProvideInfo());
		set.add(new VolcanicEngineModelProvider().getModelProvideInfo());
		set.add(new LocalModelProvider().getModelProvideInfo());
		return R.success(set);
	}


	@GetMapping("api/provider/model_type_list")
	public R<List<KeyAndValueVO>> modelTypeList(String provider){
		List<ModelInfo> modelInfos=modelProviderMap.get(provider).getModelList();
		Map<String,String> modelTypeMap = new HashMap<>();
		modelTypeMap.put("LLM","大语言模型");
		modelTypeMap.put("EMBEDDING","向量模型");
		modelTypeMap.put("STT","语音识别");
		modelTypeMap.put("TTS","语音合成");
		modelTypeMap.put("IMAGE","图片理解");
		modelTypeMap.put("TTI","图片生成");
		modelTypeMap.put("RERANKER","重排模型");
		List<KeyAndValueVO> list= new ArrayList<>();
		Map<String,List<ModelInfo>> map=modelInfos.stream().collect(Collectors.groupingBy(ModelInfo::getModelType));
		for (String key : map.keySet()) {
			list.add(new KeyAndValueVO(modelTypeMap.get(key),key));
		}
		return R.success(list);
	}

	@GetMapping("api/provider/model_list")
	public R< List<ModelInfo>> modelList(String provider, String model_type){
		List<ModelInfo> modelInfos=modelProviderMap.get(provider).getModelList();
		List<ModelInfo>  modelList=modelInfos.stream().filter(e->e.getModelType().equals(model_type)).toList();
		return R.success(modelList);
	}

	@GetMapping("api/provider/model_form")
	public R< List<ModelInfo>> modelForm(String provider, String model_type,String model_name){
		return R.success(modelProviderMap.get(provider).getModelList());
	}

	@GetMapping("api/provider/model_params_form")
	public R<List<ModelInfo>> modelParamsForm(String provider, String model_type,String model_name){
		return R.success(modelProviderMap.get(provider).getModelList());
	}

	@GetMapping("api/model")
	public R<List<ModelEntity>> models(String name, String create_user, String permission_type, String model_type){
		return R.success(modelService.models(model_type));
	}
}
