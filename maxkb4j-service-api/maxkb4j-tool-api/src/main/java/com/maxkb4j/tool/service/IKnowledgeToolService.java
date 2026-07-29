package com.maxkb4j.tool.service;

import com.maxkb4j.common.mp.entity.KnowledgeSetting;
import dev.langchain4j.service.tool.AiServiceTool;
import dev.langchain4j.service.tool.ToolProvider;

import java.util.List;

/**
 * 将知识库构建为可执行工具的 SPI。
 *
 * 故接口声明在 tool-api 供 tool 模块消费，由 Knowledge 模块实现并在运行期注入，
 * 从而避免 tool 模块反向编译依赖 Knowledge。
 *
 * @author tarzan
 */
public interface IKnowledgeToolService {

    /**
     * 根据知识库 ID 列表构建 AiServiceTool 列表
     */
    List<AiServiceTool> buildTools(List<String> KnowledgeIds, KnowledgeSetting knowledgeSetting);

    /**
     * 根据知识库 ID 列表构建 ToolProvider
     */
    ToolProvider buildToolProvider(List<String> KnowledgeIds,KnowledgeSetting knowledgeSetting);
}
