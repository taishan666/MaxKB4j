package com.maxkb4j.knowledge.tool;

import com.maxkb4j.common.mp.entity.KnowledgeSetting;
import com.maxkb4j.knowledge.dto.KnowledgeSimple;
import com.maxkb4j.knowledge.executor.KnowledgeExecutor;
import com.maxkb4j.knowledge.service.IKnowledgeService;
import com.maxkb4j.knowledge.service.IRetrieveService;
import com.maxkb4j.tool.service.IAgentToolService;
import com.maxkb4j.tool.service.IKnowledgeToolService;
import com.maxkb4j.tool.util.ToolNaming;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;
import dev.langchain4j.service.tool.AiServiceTool;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 应用工具服务实现，负责将智能体应用构建为可执行的工具。
 *
 * <p>本类实现 tool-api 中的 {@link IAgentToolService} SPI，由 tool 模块的
 * {@code ToolProviderServiceImpl} 在运行期注入，从而避免 tool 模块反向编译依赖 application。
 *
 * @author tarzan
 */
@Service
@RequiredArgsConstructor
public class KnowledgeToolServiceImpl implements IKnowledgeToolService {
    private final IRetrieveService retrieveService;
    private final IKnowledgeService knowledgeService;


    @Override
    public List<AiServiceTool> buildTools(List<String> KnowledgeIds,KnowledgeSetting knowledgeSetting) {
        if (KnowledgeIds == null || KnowledgeIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<KnowledgeSimple> knowledgeList = knowledgeService.listSimpleKnowledgeByIds(KnowledgeIds);
        if (knowledgeList.isEmpty()) {
            return Collections.emptyList();
        }
        return knowledgeList.stream()
                .map(e->toAiServiceTool(e,knowledgeSetting))
                .toList();
    }

    @Override
    public ToolProvider buildToolProvider(List<String> KnowledgeIds, KnowledgeSetting knowledgeSetting) {
        return toolProviderRequest -> {
            List<AiServiceTool> tools = buildTools(KnowledgeIds,knowledgeSetting);
            if (tools.isEmpty()) {
                return null;
            }
            return ToolProviderResult.builder().addAll(tools).build();
        };
    }

    /**
     * 将单个应用实体构建为 AiServiceTool（工具规范 + 执行器）
     */
    private AiServiceTool toAiServiceTool(KnowledgeSimple knowledge, KnowledgeSetting knowledgeSetting) {
        ToolSpecification spec = buildKnowledgeSpecification(knowledge);
        KnowledgeExecutor executor = new KnowledgeExecutor(knowledge.getId(),knowledgeSetting, retrieveService);
        return AiServiceTool.builder()
                .toolSpecification(spec)
                .toolExecutor(executor)
                .build();
    }

    /**
     * 根据知识库实体构建 ToolSpecification（知识库作为工具时的规范）：
     * 单参 message，名称遵循 knowledge_&lt;id&gt; 约定。
     */
    private ToolSpecification buildKnowledgeSpecification(KnowledgeSimple knowledgeSimple) {
        JsonObjectSchema parameters = JsonObjectSchema.builder()
                .addProperty("queries", JsonArraySchema.builder().items(JsonStringSchema.builder().build()).build())
                .required("queries")
                .build();
        return ToolSpecification.builder()
                .name(ToolNaming.buildKnowledgeName(knowledgeSimple.getId()))
                .description("**" + knowledgeSimple.getName() + "(Data retrieval tool)**" + ":" + knowledgeSimple.getDesc())
                .parameters(parameters)
                .build();
    }
}
