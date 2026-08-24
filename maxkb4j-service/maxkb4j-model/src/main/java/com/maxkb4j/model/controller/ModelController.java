package com.maxkb4j.model.controller;

import com.alibaba.fastjson.JSONArray;
import com.maxkb4j.common.annotation.SaCheckPerm;
import com.maxkb4j.common.api.R;
import com.maxkb4j.common.constant.AppConst;
import com.maxkb4j.common.enums.PermissionEnum;
import com.maxkb4j.common.util.BeanUtil;
import com.maxkb4j.core.support.permission.DataPermissionSupport;
import com.maxkb4j.model.dto.ModelCreateDTO;
import com.maxkb4j.model.dto.ModelUpdateDTO;
import com.maxkb4j.model.dto.ModelQuery;
import com.maxkb4j.model.entity.ModelEntity;
import com.maxkb4j.model.service.IModelInternalService;
import com.maxkb4j.model.vo.ModelListVO;
import com.maxkb4j.model.vo.ModelVO;
import com.maxkb4j.system.constant.AuthTargetType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import static com.maxkb4j.model.consts.ModelConstants.*;

/**
 * @author tarzan
 * @date 2024-12-25 12:22:22
 */
@RestController
@RequestMapping(AppConst.ADMIN_WORKSPACE_API)
@RequiredArgsConstructor
public class ModelController{

	private final IModelInternalService modelService;
	private final DataPermissionSupport dataPermissionSupport;

	@SaCheckPerm(PermissionEnum.MODEL_CREATE)
	@PostMapping(ApiPath.MODEL)
	public R<Boolean> createModel(@Valid @RequestBody ModelCreateDTO dto){
		ModelEntity model = BeanUtil.copy(dto, ModelEntity.class);
		return R.data(modelService.createModel(model));
	}


	@SaCheckPerm(PermissionEnum.MODEL_READ)
	@GetMapping(ApiPath.MODEL)
	public R<List<ModelVO>> models(ModelQuery  query){
		dataPermissionSupport.fill(query, AuthTargetType.MODEL);
		return R.data(modelService.models(query));
	}

	@SaCheckPerm(PermissionEnum.MODEL_READ)
	@GetMapping(ApiPath.MODEL_LIST)
	public R<Map<String, List<ModelListVO>>> modelList(ModelQuery query){
		dataPermissionSupport.fill(query, AuthTargetType.MODEL);
		List<ModelListVO> models=modelService.modelList(query);
		return R.data(Map.of(Resource.MODEL, models, Resource.SHARED_MODEL,List.of()));
	}

	@SaCheckPerm(PermissionEnum.MODEL_READ)
	@GetMapping(ApiPath.MODEL_ID)
	public R<ModelVO> getInfo(@PathVariable String id){
		ModelEntity entity = modelService.getInfo(id);
		return R.data(entity == null ? null : BeanUtil.copy(entity, ModelVO.class));
	}

	@SaCheckPerm(PermissionEnum.MODEL_DELETE)
	@DeleteMapping(ApiPath.MODEL_ID)
	public R<Boolean> delete(@PathVariable String id){
		return R.status(modelService.removeModelById(id));
	}

	@SaCheckPerm(PermissionEnum.MODEL_EDIT)
	@PutMapping(ApiPath.MODEL_ID)
	public R<ModelVO> update(@PathVariable String id,@RequestBody ModelUpdateDTO dto){
		ModelEntity model = BeanUtil.copy(dto, ModelEntity.class);
		ModelEntity entity = modelService.updateModel(id, model);
		return R.data(entity == null ? null : BeanUtil.copy(entity, ModelVO.class));
	}

	@SaCheckPerm(PermissionEnum.MODEL_READ)
	@GetMapping(ApiPath.MODEL_PARAMS_FORM)
	public R<JSONArray> modelParamsForm(@PathVariable String id){
		ModelEntity modelEntity= modelService.getById(id);
		if (modelEntity==null){
			return R.data(new JSONArray());
		}
		return R.data(modelEntity.getModelParamsForm());
	}

	@SaCheckPerm(PermissionEnum.MODEL_EDIT)
	@PutMapping(ApiPath.MODEL_PARAMS_FORM)
	public R<JSONArray> updateModelParamsForm(@PathVariable String id,@RequestBody JSONArray paramsForm){
		modelService.updateModelParamsForm(id,paramsForm);
		return R.data(paramsForm);
	}
}
