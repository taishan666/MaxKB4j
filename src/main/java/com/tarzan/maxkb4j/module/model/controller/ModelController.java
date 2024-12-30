package com.tarzan.maxkb4j.module.model.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.model.entity.ModelEntity;
import com.tarzan.maxkb4j.module.model.service.ModelService;
import com.tarzan.maxkb4j.module.model.vo.ProviderVO;
import com.tarzan.maxkb4j.tool.api.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author tarzan
 * @date 2024-12-25 12:22:22
 */
@RestController
@AllArgsConstructor
public class ModelController{

	@Autowired
	private ModelService modelService;


	@GetMapping("api/provider")
	public R<List<ProviderVO>> provider(){
		List<ProviderVO> list= new ArrayList<>();
		list.add(new ProviderVO("Azure OpenAI","model_azure_provider",""));
		list.add(new ProviderVO("通义前文","aliyun_bai_lian_model_provider",""));
		return R.success(list);
	}

	@GetMapping("api/model")
	public R<List<ModelEntity>> models(String name, String create_user, String permission_type, String model_type){
		return R.success(modelService.models(model_type));
	}
}
