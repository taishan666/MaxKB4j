package com.maxkb4j.tool.service;

import dev.langchain4j.service.tool.AiServiceTool;
import dev.langchain4j.service.tool.ToolProvider;

import java.util.List;

/**
 * 将（应用）智能体构建为可执行工具的 SPI。
 *
 * <p>该能力本质上属于 application 领域（需要查询应用、以应用身份对话），
 * 故接口声明在 tool-api 供 tool 模块消费，由 application 模块实现并在运行期注入，
 * 从而避免 tool 模块反向编译依赖 application。
 *
 * @author tarzan
 */
public interface IAgentToolService {

    /**
     * 根据应用 ID 列表构建 AiServiceTool 列表
     */
    List<AiServiceTool> buildTools(List<String> ids);

    /**
     * 根据应用 ID 列表构建 ToolProvider
     */
    ToolProvider buildToolProvider(List<String> ids);
}
