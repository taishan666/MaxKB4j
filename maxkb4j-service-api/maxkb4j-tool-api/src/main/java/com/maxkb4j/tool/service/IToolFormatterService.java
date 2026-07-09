package com.maxkb4j.tool.service;

import dev.langchain4j.service.tool.BeforeToolExecution;
import dev.langchain4j.service.tool.ToolExecution;

/**
 * 工具调用格式化服务，负责将工具执行过程格式化为前端渲染文本
 */
public interface IToolFormatterService {

    String format(BeforeToolExecution toolExecute);

    String format(ToolExecution toolExecute);

}
