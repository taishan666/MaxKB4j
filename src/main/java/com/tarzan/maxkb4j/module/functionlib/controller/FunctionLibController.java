package com.tarzan.maxkb4j.module.functionlib.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tarzan.maxkb4j.constant.AppConst;
import com.tarzan.maxkb4j.core.api.R;
import com.tarzan.maxkb4j.module.functionlib.domain.dto.FunctionDebugField;
import com.tarzan.maxkb4j.module.functionlib.domain.dto.FunctionLibDTO;
import com.tarzan.maxkb4j.module.functionlib.domain.entity.FunctionLibEntity;
import com.tarzan.maxkb4j.module.functionlib.service.FunctionLibService;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import lombok.AllArgsConstructor;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

/**
 * @author tarzan
 * @date 2025-01-25 22:00:45
 */
@RestController
@RequestMapping(AppConst.BASE_PATH)
@AllArgsConstructor
public class FunctionLibController{

	private	final FunctionLibService functionLibService;

	@GetMapping("/function_lib/{current}/{size}")
	public R<IPage<FunctionLibEntity>> page(@PathVariable int current, @PathVariable int size,String name) {
		return R.success(functionLibService.pageList(current,size,name));
	}

	@PostMapping("/function_lib")
	public R<FunctionLibEntity> functionLib(@RequestBody FunctionLibEntity dto) {
		dto.setIsActive(true);
		dto.setUserId(StpUtil.getLoginIdAsString());
		functionLibService.save(dto);
		return R.data(dto);
	}

	@PostMapping("/function_lib/debug")
	public R<String> debug(@RequestBody FunctionLibDTO dto) {
		Binding binding = new Binding();
		StringBuilder codeText=new StringBuilder(dto.getCode());
		codeText.append("\n").append("main(");
		if (CollectionUtils.isEmpty(dto.getDebugFieldList())){
			codeText.append(")");
		}else {
			for (FunctionDebugField inputField : dto.getDebugFieldList()) {
				binding.setVariable(inputField.getName(), inputField.getValue());
				codeText.append(inputField.getName()).append(",");
			}
			codeText.deleteCharAt(codeText.length()-1).append(")");
		}
		// 创建 GroovyShell 并运行脚本
		GroovyShell shell = new GroovyShell(binding);
		Object result = shell.evaluate(codeText.toString());
		String finalResult=result != null ? result.toString() : "";
		return R.data(finalResult);
	}


	@PutMapping("/function_lib/{id}")
	public R<Boolean> functionLib(@PathVariable String id,@RequestBody FunctionLibEntity dto) {
		dto.setId(id);
		return R.status(functionLibService.updateById(dto));
	}

	@DeleteMapping("/function_lib/{id}")
	public R<Boolean> functionLib(@PathVariable String id) {
		return R.status(functionLibService.removeById(id));
	}

	@PostMapping("/function_lib/pylint")
	public R<List<FunctionLibEntity>> pylint(@RequestBody JSONObject json) {
		return R.success(Collections.emptyList());
	}
}
