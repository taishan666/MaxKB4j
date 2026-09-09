package com.maxkb4j.tool.dto;

/**
 * 工具导出产物：文件名与文件字节内容
 *
 * <p>用于将导出业务逻辑与 HTTP 响应写入解耦，使导出内容构建可脱离 Servlet 容器独立测试。
 *
 * @author tarzan
 */
public record ToolExportData(String fileName, byte[] content) {
}
