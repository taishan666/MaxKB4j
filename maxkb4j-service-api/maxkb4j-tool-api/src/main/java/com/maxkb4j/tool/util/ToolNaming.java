package com.maxkb4j.tool.util;

/**
 * 工具调用名称约定：统一管理 "tool_<id>" / "agent_<id>" / "knowledge_<id>" 的生成与解析，
 * 消除生成端（ToolSpecificationBuilder / SkillToolService / ApplicationToolServiceImpl / KnowledgeToolServiceImpl）
 * 与解析端（ToolFormatterServiceImpl）之间的隐式约定耦合。
 *
 * <p>该约定被 tool、application、knowledge 三个实现模块共享，作为跨模块契约的一部分
 * 位于 tool-api，避免实现模块之间的直接依赖（依赖方向始终为实现模块 -&gt; tool-api）。
 *
 * @author tarzan
 */
public final class ToolNaming {

    public static final String SEPARATOR = "_";
    public static final String TOOL_TYPE = "tool";
    public static final String AGENT_TYPE = "agent";
    public static final String KNOWLEDGE_TYPE = "knowledge";

    /**
     * MCP 等场景下，一个工具实体（tool_&lt;id&gt;）可能对应服务端暴露的多个工具，
     * 需在基础名后追加唯一后缀避免重名（否则 langchain4j 会抛 Duplicated definition for tool）。
     * 后缀与基础名之间使用双下划线分隔；由于 id 由 ASSIGN_UUID 生成（32 位十六进制，不含下划线），
     * {@link #parse(String)} 可据首个 {@code __} 精确剥离后缀还原出 id。
     */
    public static final String SUFFIX_SEPARATOR = "__";

    private ToolNaming() {
    }

    /**
     * 构建工具类工具的调用名称：tool_<id>
     */
    public static String buildToolName(String id) {
        return TOOL_TYPE + SEPARATOR + id;
    }

    /**
     * 构建带唯一后缀的工具调用名称：tool_<id>__<suffix>。
     * 用于 MCP 一个工具实体对应多个服务端工具的场景，保证名称唯一且仍可解析回 id。
     *
     * @param id     工具实体 id
     * @param suffix 唯一后缀（已清洗为合法字符）；为空时退化为 {@link #buildToolName(String)}
     */
    public static String buildToolName(String id, String suffix) {
        String base = buildToolName(id);
        if (suffix == null || suffix.isBlank()) {
            return base;
        }
        return base + SUFFIX_SEPARATOR + suffix;
    }

    /**
     * 构建应用作为工具的调用名称：agent_<id>
     */
    public static String buildAgentName(String id) {
        return AGENT_TYPE + SEPARATOR + id;
    }

    /**
     * 构建知识库作为工具的调用名称：knowledge_<id>
     */
    public static String buildKnowledgeName(String id) {
        return KNOWLEDGE_TYPE + SEPARATOR + id;
    }

    /**
     * 解析工具调用名称。仅按第一个分隔符切分类型与 id；若 id 段含 {@link #SUFFIX_SEPARATOR}
     * 后缀（MCP 多工具场景），则剥离后缀还原出真实 id。
     *
     * @return 解析结果；名称为 null 或不符合 "&lt;type&gt;_&lt;id&gt;" 格式时返回 null
     */
    public static Ref parse(String name) {
        if (name == null) {
            return null;
        }
        int idx = name.indexOf(SEPARATOR);
        if (idx <= 0 || idx >= name.length() - 1) {
            return null;
        }
        String id = name.substring(idx + 1);
        int suffixIdx = id.indexOf(SUFFIX_SEPARATOR);
        if (suffixIdx > 0) {
            id = id.substring(0, suffixIdx);
        }
        return new Ref(name.substring(0, idx), id);
    }

    /**
     * 工具调用名称解析结果
     */
    public record Ref(String type, String id) {
    }
}
