package com.maxkb4j.tool.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.IService;
import com.maxkb4j.common.domain.dto.McpRequest;
import com.maxkb4j.tool.entity.ToolEntity;
import com.maxkb4j.tool.vo.McpToolVO;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.util.List;

public interface IToolService extends IService<ToolEntity> {

    List<McpToolVO> getMcpToolVos(JSONObject mcpServersJson);

}
