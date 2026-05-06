package com.maxkb4j.model.controller;

import com.alibaba.fastjson.JSONArray;
import com.maxkb4j.common.annotation.SaCheckPerm;
import com.maxkb4j.common.constant.AppConst;
import com.maxkb4j.common.api.R;
import com.maxkb4j.common.enums.PermissionEnum;
import com.maxkb4j.model.entity.ModelEntity;
import com.maxkb4j.model.service.ModelService;
import com.maxkb4j.model.vo.ModelVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * @author tarzan
 * @date 2024-12-25 12:22:22
 */
@RestController
@RequestMapping(AppConst.ADMIN_WORKSPACE_API)
@RequiredArgsConstructor
public class ModelController{

	private final ModelService modelService;

	@SaCheckPerm(PermissionEnum.MODEL_CREATE)
	@PostMapping("/model")
	public R<Boolean> createModel(@RequestBody ModelEntity model){
		return R.success(modelService.createModel(model));
	}


	@SaCheckPerm(PermissionEnum.MODEL_READ)
	@GetMapping("/model")
	public R<List<ModelVO>> models(String name, String createUser, String modelType, String provider){
		return R.success(modelService.models(name,createUser,modelType,provider));
	}

	@SaCheckPerm(PermissionEnum.MODEL_READ)
	@GetMapping("/model_list")
	public R<Map<String, List<ModelVO>>> modelList(String name, String createUser, String modelType, String provider){
		List<ModelVO> models=modelService.models(name,createUser,modelType,provider);
		return R.success(Map.of("model", models, "shared_model",List.of()));
	}

	@SaCheckPerm(PermissionEnum.MODEL_READ)
	@GetMapping("/model/{id}")
	public R<ModelEntity> getInfo(@PathVariable String id){
		return R.success(modelService.getInfo(id));
	}

	@SaCheckPerm(PermissionEnum.MODEL_DELETE)
	@DeleteMapping("/model/{id}")
	public R<Boolean> delete(@PathVariable String id){
		return R.success(modelService.removeModelById(id));
	}

	@SaCheckPerm(PermissionEnum.MODEL_EDIT)
	@PutMapping("/model/{id}")
	public R<ModelEntity> update(@PathVariable String id,@RequestBody ModelEntity model){
		return R.success(modelService.updateModel(id,model));
	}

	@SaCheckPerm(PermissionEnum.MODEL_READ)
	@GetMapping("/model/{id}/model_params_form")
	public R<JSONArray> modelParamsForm(@PathVariable String id){
		ModelEntity modelEntity= modelService.getById(id);
		if (modelEntity==null){
			return R.data(new JSONArray());
		}
		return R.success(modelEntity.getModelParamsForm());
	}

	@SaCheckPerm(PermissionEnum.MODEL_EDIT)
	@PutMapping("/model/{id}/model_params_form")
	public R<JSONArray> updateModelParamsForm(@PathVariable String id,@RequestBody JSONArray array){
		ModelEntity modelEntity= new ModelEntity();
		modelEntity.setId(id);
		modelEntity.setModelParamsForm(array);
		modelService.updateById(modelEntity);
		return R.success(modelEntity.getModelParamsForm());
	}
}
