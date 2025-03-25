package com.tarzan.maxkb4j.module.functionlib.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tarzan.maxkb4j.module.functionlib.entity.FunctionLibEntity;
import com.tarzan.maxkb4j.module.functionlib.service.FunctionLibService;
import com.tarzan.maxkb4j.core.api.R;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

/**
 * @author tarzan
 * @date 2025-01-25 22:00:45
 */
@RestController
@AllArgsConstructor
public class FunctionLibController{

	private	final FunctionLibService functionLibService;

	@GetMapping("api/function_lib/{current}/{size}")
	public R<IPage<FunctionLibEntity>> page(@PathVariable int current, @PathVariable int size,String name) {
		return R.success(functionLibService.pageList(current,size,name));
	}

	@PostMapping("api/function_lib")
	public R<FunctionLibEntity> functionLib(@RequestBody FunctionLibEntity dto) {
		dto.setIsActive(true);
		dto.setUserId(StpUtil.getLoginIdAsString());
		functionLibService.save(dto);
		return R.data(dto);
	}

	@PutMapping("api/function_lib/{id}")
	public R<Boolean> functionLib(@PathVariable String id,@RequestBody FunctionLibEntity dto) {
		dto.setId(id);
		return R.status(functionLibService.updateById(dto));
	}

	@DeleteMapping("api/function_lib/{id}")
	public R<Boolean> functionLib(@PathVariable String id) {
		return R.status(functionLibService.removeById(id));
	}

	@PostMapping("api/function_lib/pylint")
	public R<List<FunctionLibEntity>> pylint(@RequestBody JSONObject json) {
		return R.success(Collections.emptyList());
	}
}
