package com.tarzan.maxkb4j.module.mcplib.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tarzan.maxkb4j.core.api.R;
import com.tarzan.maxkb4j.module.mcplib.entity.McpLibEntity;
import com.tarzan.maxkb4j.module.mcplib.service.McpLibService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @author tarzan
 * @date 2025-01-25 22:00:45
 */
@RestController
@AllArgsConstructor
public class McpLibController {

	private	final McpLibService mcpLibService;

	@GetMapping("api/mcp_lib/{current}/{size}")
	public R<IPage<McpLibEntity>> page(@PathVariable int current, @PathVariable int size, String name) {
		return R.success(mcpLibService.pageList(current,size,name));
	}

	@PostMapping("api/mcp_lib")
	public R<McpLibEntity> functionLib(@RequestBody McpLibEntity dto) {
		dto.setIsActive(true);
		dto.setUserId(StpUtil.getLoginIdAsString());
		mcpLibService.save(dto);
		return R.data(dto);
	}

	@PutMapping("api/mcp_lib/{id}")
	public R<Boolean> functionLib(@PathVariable String id,@RequestBody McpLibEntity dto) {
		dto.setId(id);
		return R.status(mcpLibService.updateById(dto));
	}

	@DeleteMapping("api/mcp_lib/{id}")
	public R<Boolean> functionLib(@PathVariable String id) {
		return R.status(mcpLibService.removeById(id));
	}

}
