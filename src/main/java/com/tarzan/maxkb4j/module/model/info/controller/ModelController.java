package com.tarzan.maxkb4j.module.model.info.controller;

import com.alibaba.fastjson.JSONArray;
import com.tarzan.maxkb4j.common.aop.SaCheckPerm;
import com.tarzan.maxkb4j.common.api.R;
import com.tarzan.maxkb4j.common.constant.AppConst;
import com.tarzan.maxkb4j.module.model.info.entity.ModelEntity;
import com.tarzan.maxkb4j.module.model.info.service.ModelService;
import com.tarzan.maxkb4j.module.model.info.vo.ModelVO;
import com.tarzan.maxkb4j.module.system.user.enums.PermissionEnum;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * @author tarzan
 * @date 2024-12-25 12:22:22
 */
@RestController
@RequestMapping(AppConst.ADMIN_API+"/workspace/default")
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
	public R<List<ModelVO>> models(String name, String createUser, String modelType,String provider){
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
