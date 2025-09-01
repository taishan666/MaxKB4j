package com.tarzan.maxkb4j.module.model.info.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.constant.AppConst;
import com.tarzan.maxkb4j.core.api.R;
import com.tarzan.maxkb4j.module.model.info.entity.ModelEntity;
import com.tarzan.maxkb4j.module.model.info.service.ModelService;
import com.tarzan.maxkb4j.module.model.info.vo.ModelVO;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author tarzan
 * @date 2024-12-25 12:22:22
 */
@RestController
@RequestMapping(AppConst.ADMIN_PATH+"/workspace/default")
@AllArgsConstructor
public class ModelController{

	private final ModelService modelService;


	//@SaCheckPermission("MODEL:CREATE")
	@PostMapping("/model")
	public R<Boolean> createModel(@RequestBody ModelEntity model){
		return R.success(modelService.createModel(model));
	}



	@GetMapping("/model")
	public R<List<ModelVO>> models(String name, String createUser, String modelType,String provider){
		return R.success(modelService.models(name,createUser,modelType,provider));
	}

	@GetMapping("/model_list")
	public R<JSONObject> modelList(String name, String createUser, String modelType, String provider){
		List<ModelVO> models=modelService.models(name,createUser,modelType,provider);
		JSONObject result=new JSONObject();
		result.put("model",models);
		result.put("shared_model",models);
		return R.success(result);
	}

	//@SaCheckPermission("MODEL:READ")
	@GetMapping("/model/{id}")
	public R<ModelEntity> get(@PathVariable String id){
		return R.success(modelService.getById(id));
	}

	//@SaCheckPermission("MODEL:DELETE")
	@DeleteMapping("/model/{id}")
	public R<Boolean> delete(@PathVariable String id){
		return R.success(modelService.removeById(id));
	}

	//@SaCheckPermission("MODEL:EDIT")
	@PutMapping("/model/{id}")
	public R<ModelEntity> update(@PathVariable String id,@RequestBody ModelEntity model){
		return R.success(modelService.updateModel(id,model));
	}

	//@SaCheckPermission("MODEL:READ")
	@GetMapping("/model/{id}/model_params_form")
	public R<JSONArray> modelParamsForm(@PathVariable String id){
		ModelEntity modelEntity= modelService.getById(id);
		return R.success(modelEntity.getModelParamsForm());
	}

	//@SaCheckPermission("MODEL:EDIT")
	@PutMapping("/model/{id}/model_params_form")
	public R<JSONArray> updateModelParamsForm(@PathVariable String id,@RequestBody JSONArray array){
		ModelEntity modelEntity= new ModelEntity();
		modelEntity.setId(id);
		modelEntity.setModelParamsForm(array);
		modelService.updateById(modelEntity);
		return R.success(modelEntity.getModelParamsForm());
	}
}
