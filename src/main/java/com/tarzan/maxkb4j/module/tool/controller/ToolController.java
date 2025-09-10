package com.tarzan.maxkb4j.module.tool.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tarzan.maxkb4j.constant.AppConst;
import com.tarzan.maxkb4j.core.api.R;
import com.tarzan.maxkb4j.module.tool.domain.dto.ToolDTO;
import com.tarzan.maxkb4j.module.tool.domain.dto.ToolDebugField;
import com.tarzan.maxkb4j.module.tool.domain.dto.ToolQuery;
import com.tarzan.maxkb4j.module.tool.domain.entity.ToolEntity;
import com.tarzan.maxkb4j.module.tool.domain.vo.ToolVO;
import com.tarzan.maxkb4j.module.tool.service.ToolService;
import com.tarzan.maxkb4j.util.StringUtil;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import lombok.AllArgsConstructor;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author tarzan
 * @date 2025-01-25 22:00:45
 */
@RestController
@RequestMapping(AppConst.ADMIN_PATH)
@AllArgsConstructor
public class ToolController {

	private	final ToolService toolService;

	@GetMapping("/workspace/default/tool/{current}/{size}")
	public R<IPage<ToolVO>> page(@PathVariable int current, @PathVariable int size, ToolQuery query) {
		return R.success(toolService.pageList(current,size,query));
	}

	@GetMapping("/workspace/default/tool")
	public R<Map<String,List<ToolEntity>>> list1(String folderId, String toolType) {
		LambdaQueryWrapper<ToolEntity> wrapper= Wrappers.lambdaQuery();
		wrapper.eq(ToolEntity::getToolType, toolType);
		wrapper.eq(ToolEntity::getIsActive, true);
		List<ToolEntity> tools=toolService.list(wrapper);
		return R.success(Map.of("folders", List.of(), "tools", tools));
	}

	@GetMapping("/workspace/{type}/tool")
	public R<List<ToolEntity>> list(@PathVariable String type, String name) {
		LambdaQueryWrapper<ToolEntity> wrapper= Wrappers.lambdaQuery();
		wrapper.eq(ToolEntity::getToolType, type.toUpperCase());
		if (StringUtil.isNotBlank( name)){
			wrapper.like(ToolEntity::getName, name);
		}
		return R.success(toolService.list(wrapper));
	}

	@GetMapping("/workspace/default/tool/tool_list")
	public R<Map<String,List<ToolEntity>>> toolList(String scope, String toolType) {
		LambdaQueryWrapper<ToolEntity> wrapper= Wrappers.lambdaQuery();
		wrapper.eq(ToolEntity::getToolType, toolType);
		wrapper.eq(ToolEntity::getScope, scope);
		List<ToolEntity> tools=toolService.list(wrapper);
		return R.success(Map.of("tools", tools, "shared_tools", tools));
	}

	@PostMapping("/workspace/default/tool/{templateId}/add_internal_tool")
	public R<ToolEntity> addInternalTool(@PathVariable String templateId) {
		ToolEntity entity=toolService.getById(templateId);
		entity.setId(null);
		entity.setUserId(StpUtil.getLoginIdAsString());
		Date now = new Date();
		entity.setCreateTime(now);
		entity.setScope("WORKSPACE");
		entity.setToolType("CUSTOM");
		entity.setUpdateTime(now);
		toolService.saveInfo(entity);
		return R.data(entity);
	}

	@PostMapping("/workspace/default/tool")
	public R<ToolEntity> functionLib(@RequestBody ToolEntity dto) {
		dto.setIsActive(true);
		dto.setUserId(StpUtil.getLoginIdAsString());
		dto.setScope("WORKSPACE");
		toolService.saveInfo(dto);
		return R.data(dto);
	}

	@PostMapping("/workspace/default/tool/debug")
	public R<String> debug(@RequestBody ToolDTO dto) {
		Binding binding = new Binding();
		StringBuilder codeText=new StringBuilder(dto.getCode());
		codeText.append("\n").append("main(");
		if (CollectionUtils.isEmpty(dto.getDebugFieldList())){
			codeText.append(")");
		}else {
			for (ToolDebugField inputField : dto.getDebugFieldList()) {
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

	@GetMapping("/workspace/default/tool/{id}")
	public R<ToolEntity> get(@PathVariable String id) {
		return R.data(toolService.getById(id));
	}

	@PutMapping("/workspace/default/tool/{id}")
	public R<Boolean> tool(@PathVariable String id,@RequestBody ToolEntity dto) {
		dto.setId(id);
		return R.status(toolService.updateById(dto));
	}

	@DeleteMapping("/workspace/default/tool/{id}")
	public R<Boolean> tool(@PathVariable String id) {
		return R.status(toolService.removeById(id));
	}

	@PostMapping("/workspace/default/tool/pylint")
	public R<List<ToolEntity>> pylint(@RequestBody JSONObject json) {
		return R.success(Collections.emptyList());
	}
}
