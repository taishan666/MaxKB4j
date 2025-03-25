package com.tarzan.maxkb4j.module.model.info.controller;

import com.alibaba.fastjson.JSONArray;
import com.tarzan.maxkb4j.module.model.info.entity.ModelEntity;
import com.tarzan.maxkb4j.module.model.info.service.ModelService;
import com.tarzan.maxkb4j.module.model.info.vo.KeyAndValueVO;
import com.tarzan.maxkb4j.module.model.info.vo.ModelVO;
import com.tarzan.maxkb4j.module.model.provider.IModelProvider;
import com.tarzan.maxkb4j.module.model.provider.ModelInfo;
import com.tarzan.maxkb4j.module.model.provider.ModelProvideInfo;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelProviderEnum;
import com.tarzan.maxkb4j.module.model.provider.enums.ModelTypeEnum;
import com.tarzan.maxkb4j.module.model.provider.impl.aliyunModelProvider.AliYunBaiLianModelProvider;
import com.tarzan.maxkb4j.module.model.provider.impl.azuremodelprovider.AzureModelProvider;
import com.tarzan.maxkb4j.module.model.provider.impl.ollamamodelprovider.OLlamaModelProvider;
import com.tarzan.maxkb4j.module.model.provider.impl.openaimodelprovider.OpenaiModelProvider;
import com.tarzan.maxkb4j.module.model.provider.impl.wenxinmodelprovider.WenXinModelProvider;
import com.tarzan.maxkb4j.module.model.provider.impl.xinferencemodelprovider.XInferenceModelProvider;
import com.tarzan.maxkb4j.module.model.provider.impl.zhipumodelprovider.ZhiPuModelProvider;
import com.tarzan.maxkb4j.module.model.provider.vo.ModelInputVO;
import com.tarzan.maxkb4j.core.api.R;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
@AllArgsConstructor
public class ModelController{

	private final ModelService modelService;

    @GetMapping("api/provider")
	public R<Set<ModelProvideInfo>> provider(){
		Set<ModelProvideInfo> set= new HashSet<>();
		set.add(new AliYunBaiLianModelProvider().getModelProvideInfo());
		//set.add(new AwsBedrockModelProvider().getModelProvideInfo());
		set.add(new AzureModelProvider().getModelProvideInfo());
		//set.add(new DeepSeekModelProvider().getModelProvideInfo());
		//set.add(new KimiModelProvider().getModelProvideInfo());
		set.add(new OLlamaModelProvider().getModelProvideInfo());
		set.add(new OpenaiModelProvider().getModelProvideInfo());
		//set.add(new GeminiModelProvider().getModelProvideInfo());
	//	set.add(new QwenModelProvider().getModelProvideInfo());
		//set.add(new TencentModelProvider().getModelProvideInfo());
		set.add(new WenXinModelProvider().getModelProvideInfo());
		//set.add(new XfModelProvider().getModelProvideInfo());
		set.add(new ZhiPuModelProvider().getModelProvideInfo());
	//	set.add(new VLlmModelProvider().getModelProvideInfo());
		set.add(new XInferenceModelProvider().getModelProvideInfo());
	//	set.add(new VolcanicEngineModelProvider().getModelProvideInfo());
	//	set.add(new LocalModelProvider().getModelProvideInfo());
		return R.success(set);
	}


	@GetMapping("api/provider/model_type_list")
	public R<List<KeyAndValueVO>> modelTypeList(String provider){
		IModelProvider modelProvider=ModelProviderEnum.get(provider);
		List<ModelInfo> modelInfos=modelProvider.getModelList();
		List<KeyAndValueVO> list= ModelTypeEnum.getModelTypeList();
		Map<String,List<ModelInfo>> map=modelInfos.stream().collect(Collectors.groupingBy(ModelInfo::getModelType));
		Set<String> keys=map.keySet();
        list.removeIf(e -> !keys.contains(e.getValue()));
		return R.success(list);
	}

	@GetMapping("api/provider/model_list")
	public R<List<ModelInfo>> modelList(String provider, String modelType){
		IModelProvider modelProvider=ModelProviderEnum.get(provider);
		List<ModelInfo> modelInfos=modelProvider.getModelList();
		List<ModelInfo>  modelList=modelInfos.stream().filter(e->e.getModelType().equals(modelType)).toList();
		return R.success(modelList);
	}

	@GetMapping("api/provider/model_form")
	public R<List<ModelInputVO>> modelForm(String provider, String modelType, String modelName){
		IModelProvider modelProvider=ModelProviderEnum.get(provider);
		return R.success(modelProvider.getModelCredential(modelType, modelName).toForm());
	}

	@GetMapping("api/provider/model_params_form")
	public R<List<ModelInputVO>> modelParamsForm(String provider, String modelType,String modelName){
		IModelProvider modelProvider=ModelProviderEnum.get(provider);
		return R.success(modelProvider.getModelCredential(modelType, modelName).getModelParamsSettingForm());
	}

	@PostMapping("api/model")
	public R<Boolean> createModel(@RequestBody ModelEntity model){
		return R.success(modelService.createModel(model));
	}

	@GetMapping("api/model")
	public R<List<ModelVO>> models(String name, String createUser, String permissionType, String modelType,String provider){
		return R.success(modelService.models(name,createUser,permissionType,modelType,provider));
	}

	@GetMapping("api/model/{id}")
	public R<ModelEntity> get(@PathVariable String id){
		return R.success(modelService.getById(id));
	}

	@DeleteMapping("api/model/{id}")
	public R<Boolean> delete(@PathVariable String id){
		return R.success(modelService.removeById(id));
	}

	@PutMapping("api/model/{id}")
	public R<ModelEntity> update(@PathVariable String id,@RequestBody ModelEntity model){
		return R.success(modelService.updateModel(id,model));
	}

	@GetMapping("api/model/{id}/model_params_form")
	public R<JSONArray> modelParamsForm(@PathVariable String id){
		ModelEntity modelEntity= modelService.getById(id);
		return R.success(modelEntity.getModelParamsForm());
	}

	@PutMapping("api/model/{id}/model_params_form")
	public R<JSONArray> updateModelParamsForm(@PathVariable String id,@RequestBody JSONArray array){
		ModelEntity modelEntity= new ModelEntity();
		modelEntity.setId(id);
		modelEntity.setModelParamsForm(array);
		modelService.updateById(modelEntity);
		return R.success(modelEntity.getModelParamsForm());
	}
}
