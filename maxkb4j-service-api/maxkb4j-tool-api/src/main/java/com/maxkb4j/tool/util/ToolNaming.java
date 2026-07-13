package com.maxkb4j.tool.util;

/**
 * 工具调用名称约定：统一管理 "tool_<id>" / "agent_<id>" 的生成与解析，
 * 消除生成端（ToolSpecificationBuilder / SkillToolService / ApplicationToolServiceImpl）与解析端（ToolFormatterService）之间的隐式约定耦合。
 *
 * <p>位于 tool-api 以便 tool 实现与 application 实现共享同一约定，避免 tool 模块反向依赖 application。
 *
 * @author tarzan
 */
public final class ToolNaming {

    public static final String SEPARATOR = "_";
    public static final String TOOL_TYPE = "tool";
    public static final String AGENT_TYPE = "agent";

    private ToolNaming() {
    }

    /**
     * 构建工具类工具的调用名称：tool_<id>
     */
    public static String buildToolName(String id) {
        return TOOL_TYPE + SEPARATOR + id;
    }

    /**
     * 构建应用作为工具的调用名称：agent_<id>
     */
    public static String buildAgentName(String id) {
        return AGENT_TYPE + SEPARATOR + id;
    }

    /**
     * 解析工具调用名称。仅按第一个分隔符切分，id 中含分隔符也能正确往返。
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
        return new Ref(name.substring(0, idx), name.substring(idx + 1));
    }

    /**
     * 工具调用名称解析结果
     */
    public record Ref(String type, String id) {
    }
}
