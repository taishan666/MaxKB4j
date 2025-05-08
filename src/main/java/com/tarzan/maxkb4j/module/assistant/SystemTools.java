package com.tarzan.maxkb4j.module.assistant;

import cn.hutool.core.date.DateUtil;
import dev.langchain4j.agent.tool.Tool;

public class SystemTools {

    @Tool("获取当前时间")
    public String getCurrentTime() {
        return DateUtil.now();
    }
}
