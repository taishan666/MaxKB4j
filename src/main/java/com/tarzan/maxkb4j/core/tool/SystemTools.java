package com.tarzan.maxkb4j.core.tool;

import cn.hutool.core.date.DateUtil;
import dev.langchain4j.agent.tool.Tool;

import java.sql.SQLException;

public class SystemTools {

    @Tool("获取当前时间")
    public String getCurrentTime() {
        return DateUtil.now();
    }


    public static  void main(String[] args) throws SQLException {
    }

}
