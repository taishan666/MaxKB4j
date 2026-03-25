package com.maxkb4j.application.util;

import dev.langchain4j.agent.tool.Tool;

public class ShellTool {

    @Tool("执行一个 shell 命令并返回结果")
    public String runShellCommand(String command) {
        // ... 你的业务逻辑
        return "命令执行结果";
    }
}
