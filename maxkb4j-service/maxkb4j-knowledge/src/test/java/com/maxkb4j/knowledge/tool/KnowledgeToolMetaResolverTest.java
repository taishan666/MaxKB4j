package com.maxkb4j.knowledge.tool;

import com.maxkb4j.knowledge.dto.KnowledgeSimple;
import com.maxkb4j.knowledge.service.IKnowledgeService;
import com.maxkb4j.tool.vo.ToolRenderMeta;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

/**
 * {@link KnowledgeToolMetaResolver} 元数据解析测试。
 *
 * <p>回归 {@code KnowledgeSimple#type} 由 String 调整为 Integer 的修改：
 * Integer 类型在工具渲染边界经 {@code String.valueOf} 转换回字符串。</p>
 */
@ExtendWith(MockitoExtension.class)
class KnowledgeToolMetaResolverTest {

    private static final String AGENT_ID = "knowledge-1";

    @Mock
    private IKnowledgeService knowledgeService;
    @InjectMocks
    private KnowledgeToolMetaResolver resolver;

    @Test
    void resolve_convertsIntegerTypeToStringForRender() {
        KnowledgeSimple knowledge = new KnowledgeSimple();
        knowledge.setId(AGENT_ID);
        knowledge.setName("产品知识库");
        knowledge.setType(1);
        when(knowledgeService.getSimpleKnowledgeById(AGENT_ID)).thenReturn(knowledge);

        ToolRenderMeta meta = resolver.resolve(AGENT_ID);

        // ToolRenderMeta(icon, name)：type 作为图标标识、知识库名称作为展示名
        assertEquals("1", meta.icon());
        assertEquals("产品知识库", meta.name());
    }

    @Test
    void resolve_unknownKnowledge_returnsNull() {
        when(knowledgeService.getSimpleKnowledgeById(AGENT_ID)).thenReturn(null);

        assertNull(resolver.resolve(AGENT_ID));
    }
}
