package com.maxkb4j.tool.vo;

/**
 * 工具调用渲染所需的展示元数据（图标、名称）。
 *
 * <p>用于 {@code ToolFormatterServiceImpl} 渲染工具调用结果时回填前端展示信息，
 * 对 agent 类工具（应用即工具）的元数据由 application 模块通过 SPI 解析。
 *
 * @author tarzan
 */
public record ToolRenderMeta(String icon, String name) {
}
