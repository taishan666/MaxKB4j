package com.maxkb4j.model.controller;

import com.alibaba.fastjson.JSONArray;
import com.maxkb4j.common.annotation.SaCheckPerm;
import com.maxkb4j.common.api.R;
import com.maxkb4j.common.constant.AppConst;
import com.maxkb4j.common.constant.RoleType;
import com.maxkb4j.common.context.UserContext;
import com.maxkb4j.common.enums.PermissionEnum;
import com.maxkb4j.common.util.BeanUtil;
import com.maxkb4j.model.dto.ModelQuery;
import com.maxkb4j.model.entity.ModelEntity;
import com.maxkb4j.model.service.IModelInternalService;
import com.maxkb4j.model.vo.ModelVO;
import com.maxkb4j.system.constant.AuthTargetType;
import com.maxkb4j.user.service.IUserResourcePermissionService;
import com.maxkb4j.user.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author tarzan
 * @date 2024-12-25 12:22:22
 */
@RestController
@RequestMapping(AppConst.ADMIN_WORKSPACE_API)
@RequiredArgsConstructor
public class ModelController{

	private final IModelInternalService modelService;
	private final UserContext userContext;
	private final IUserResourcePermissionService userResourcePermissionService;
	private final IUserService userService;

	@SaCheckPerm(PermissionEnum.MODEL_CREATE)
	@PostMapping("/model")
	public R<Boolean> createModel(@RequestBody ModelEntity model){
		return R.data(modelService.createModel(model));
	}


	@SaCheckPerm(PermissionEnum.MODEL_READ)
	@GetMapping("/model")
	public R<List<ModelVO>> models(ModelQuery  query){
		String loginId = userContext.getUserId();
		List<String> targetIds = userResourcePermissionService.getTargetIds(AuthTargetType.MODEL, loginId);
		Set<String> role = userService.getRoleById(loginId);
		query.setIsAdmin(role.contains(RoleType.ADMIN));
		query.setTargetIds(targetIds);
		return R.data(modelService.modelList(query));
	}

	@SaCheckPerm(PermissionEnum.MODEL_READ)
	@GetMapping("/model_list")
	public R<Map<String, List<ModelVO>>> modelList(ModelQuery query){
		String loginId = userContext.getUserId();
		List<String> targetIds = userResourcePermissionService.getTargetIds(AuthTargetType.MODEL, loginId);
		Set<String> role = userService.getRoleById(loginId);
		query.setIsAdmin(role.contains(RoleType.ADMIN));
		query.setTargetIds(targetIds);
		List<ModelVO> models=modelService.modelList(query);
		return R.data(Map.of("model", models, "shared_model",List.of()));
	}

	@SaCheckPerm(PermissionEnum.MODEL_READ)
	@GetMapping("/model/{id}")
	public R<ModelVO> getInfo(@PathVariable String id){
		ModelEntity entity = modelService.getInfo(id);
		return R.data(entity == null ? null : BeanUtil.copy(entity, ModelVO.class));
	}

	@SaCheckPerm(PermissionEnum.MODEL_DELETE)
	@DeleteMapping("/model/{id}")
	public R<Boolean> delete(@PathVariable String id){
		return R.status(modelService.removeModelById(id));
	}

	@SaCheckPerm(PermissionEnum.MODEL_EDIT)
	@PutMapping("/model/{id}")
	public R<ModelVO> update(@PathVariable String id,@RequestBody ModelEntity model){
		ModelEntity entity = modelService.updateModel(id, model);
		return R.data(entity == null ? null : BeanUtil.copy(entity, ModelVO.class));
	}

	@SaCheckPerm(PermissionEnum.MODEL_READ)
	@GetMapping("/model/{id}/model_params_form")
	public R<JSONArray> modelParamsForm(@PathVariable String id){
		ModelEntity modelEntity= modelService.getById(id);
		if (modelEntity==null){
			return R.data(new JSONArray());
		}
		return R.data(modelEntity.getModelParamsForm());
	}

	@SaCheckPerm(PermissionEnum.MODEL_EDIT)
	@PutMapping("/model/{id}/model_params_form")
	public R<JSONArray> updateModelParamsForm(@PathVariable String id,@RequestBody JSONArray paramsForm){
		modelService.updateModelParamsForm(id,paramsForm);
		return R.data(paramsForm);
	}
}
