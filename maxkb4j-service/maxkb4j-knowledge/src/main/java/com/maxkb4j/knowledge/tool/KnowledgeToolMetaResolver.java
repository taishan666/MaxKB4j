package com.maxkb4j.knowledge.tool;

import com.maxkb4j.knowledge.dto.KnowledgeSimple;
import com.maxkb4j.knowledge.service.IKnowledgeService;
import com.maxkb4j.tool.service.IKnowledgeToolMetaResolver;
import com.maxkb4j.tool.vo.ToolRenderMeta;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * agent 类工具调用展示元数据解析实现。
 *
 * <p>实现 tool-api 中的 {@link IKnowledgeToolMetaResolver} SPI，由 tool 模块的
 * {@code ToolFormatterServiceImpl} 在运行期注入，查询应用图标/名称用于渲染 agent 工具调用。
 *
 * @author tarzan
 */
@Service
@RequiredArgsConstructor
public class KnowledgeToolMetaResolver implements IKnowledgeToolMetaResolver {

    private final IKnowledgeService knowledgeService;

    @Override
    public ToolRenderMeta resolve(String agentId) {
        KnowledgeSimple knowledge = knowledgeService.getSimpleKnowledgeById(agentId);
        return knowledge == null ? null : new ToolRenderMeta(knowledge.getType(), knowledge.getName());
    }
}
