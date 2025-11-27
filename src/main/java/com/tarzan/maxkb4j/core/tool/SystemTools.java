package com.tarzan.maxkb4j.core.tool;

import cn.hutool.core.date.DateUtil;
import com.tarzan.maxkb4j.common.util.DatabaseUtil;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.experimental.rag.content.retriever.sql.SqlDatabaseContentRetriever;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.query.Query;

import javax.sql.DataSource;
import java.util.List;

public class SystemTools {

    @Tool("获取当前时间")
    public String getCurrentTime() {
        return DateUtil.now();
    }

/*    @Tool("用于执行数学表达式的工具，通过 js 的 expr-eval 库运行表达式并返回结果。")
    public String mathematicalExpression(@P("expression") String expression) {
        Expression engine = new ExpressionBuilder(expression)
                .build();
        double result = engine.evaluate();
        return "result is " + result;
    }*/


    public static  void main(String[] args) {
        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey("")
                .modelName("")
                .build();

        DataSource dataSource =DatabaseUtil.getDataSource("h2", "localhost", 3306, "root", "123456", "test");
        SqlDatabaseContentRetriever sqlDatabaseContentRetriever = SqlDatabaseContentRetriever.builder().chatModel(chatModel).dataSource(dataSource).maxRetries(2).build();
        Query naturalLanguageQuery=Query.from("查询所有用户");
        List<Content> contents = sqlDatabaseContentRetriever.retrieve(naturalLanguageQuery);
    }

}
