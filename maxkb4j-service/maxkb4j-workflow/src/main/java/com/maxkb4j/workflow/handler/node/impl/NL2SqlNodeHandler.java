package com.maxkb4j.workflow.handler.node.impl;

import com.maxkb4j.core.assistant.NL2SqlAssistant;
import com.maxkb4j.core.langchain4j.AiChatMemory;
import com.maxkb4j.core.langchain4j.AiServiceFactory;
import com.maxkb4j.core.util.DatabaseUtil;
import com.maxkb4j.model.service.IModelProviderService;
import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.AbsNodeHandler;
import com.maxkb4j.workflow.model.ModelConfig;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.NL2SqlNode;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

@Slf4j
@NodeHandlerType(NodeType.NL2SQL)
@RequiredArgsConstructor
@Component
public class NL2SqlNodeHandler extends AbsNodeHandler {

    private final IModelProviderService modelFactory;

    @Override
    protected NodeResult doExecute(IWorkflow workflow, AbsNode node) throws Exception {
        NL2SqlNode.NodeParams params = parseParams(node, NL2SqlNode.NodeParams.class);
        NL2SqlNode.DatabaseSetting databaseSetting = params.getDatabaseSetting();
        List<String> fields = params.getQuestionReferenceAddress();
        String question = getReferenceFieldAsString(workflow, fields);
        ModelConfig modelConfig = resolveModelConfig(workflow, params);
        ChatModel chatModel = modelFactory.buildChatModel(modelConfig.getModelId(), modelConfig.getModelParamsSetting());
        DataSource dataSource = DatabaseUtil.getDataSource(
                databaseSetting.getType(), databaseSetting.getHost(), databaseSetting.getPort(),
                databaseSetting.getUsername(), databaseSetting.getPassword(), databaseSetting.getDatabase());
        try {
            String sqlDialect = DatabaseUtil.getSqlDialect(dataSource);
            String databaseStructure = DatabaseUtil.generateDDL(dataSource);
            List<ChatMessage> historyMessages = workflow.getHistoryMessages(params.getDialogueNumber(), params.getDialogueType(), node.getRuntimeNodeId());
            String chatId = (String) workflow.getGlobalContext().get("chatId");
            NL2SqlAssistant assistant = AiServiceFactory.builder(NL2SqlAssistant.class)
                    .chatModel(chatModel)
                    .chatMemory(AiChatMemory.withMessages(chatId, historyMessages))
                    .build();

            Result<String> result = assistant.generateSqlQuery(sqlDialect, databaseStructure, question);
            String sql = DatabaseUtil.cleanSql(result.content());
            // 执行清洗后的 SQL（去除 markdown 代码块围栏），而非 LLM 原始输出
            String sqlResult = DatabaseUtil.executeSqlQuery(sql, dataSource);

            putDetail(node, "question", question);
            recordTokenUsage(node, result.tokenUsage());
            return new NodeResult(Map.of("sql", sql, "result", sqlResult));
        } finally {
            // getDataSource 每次调用都会新建 HikariDataSource（含连接池与后台线程），必须关闭，否则持续泄漏连接与线程
            if (dataSource instanceof AutoCloseable closeable) {
                try {
                    closeable.close();
                } catch (Exception e) {
                    log.warn("Failed to close NL2SQL datasource", e);
                }
            }
        }
    }
}
