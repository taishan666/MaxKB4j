package com.maxkb4j.workflow.handler.node.impl;

import com.maxkb4j.core.util.DatabaseUtil;
import com.maxkb4j.core.assistant.NL2SqlAssistant;
import com.maxkb4j.core.langchain4j.AppChatMemory;
import com.maxkb4j.core.langchain4j.AssistantServices;
import com.maxkb4j.model.service.IModelProviderService;
import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.AbstractNodeHandler;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.NL2SqlNode;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.output.TokenUsage;
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
public class NL2SqlNodeHandler extends AbstractNodeHandler<NL2SqlNode.NodeParams> {

    private final IModelProviderService modelFactory;

    @Override
    protected Class<NL2SqlNode.NodeParams> getParamsClass() {
        return NL2SqlNode.NodeParams.class;
    }

    @Override
    protected NodeResult doExecute(Workflow workflow, AbsNode node, NL2SqlNode.NodeParams params) throws Exception {
        NL2SqlNode.DatabaseSetting databaseSetting = params.getDatabaseSetting();
        List<String> fields = params.getQuestionReferenceAddress();
        String question = getReferenceFieldAsString(workflow, fields);

        ChatModel chatModel = modelFactory.buildChatModel(params.getModelId(), params.getModelParamsSetting());
        DataSource dataSource = DatabaseUtil.getDataSource(
                databaseSetting.getType(), databaseSetting.getHost(), databaseSetting.getPort(),
                databaseSetting.getUsername(), databaseSetting.getPassword(), databaseSetting.getDatabase());

        String sqlDialect = DatabaseUtil.getSqlDialect(dataSource);
        String databaseStructure = DatabaseUtil.generateDDL(dataSource);
        List<ChatMessage> historyMessages = workflow.getHistoryMessages(params.getDialogueNumber(), params.getDialogueType(), node.getRuntimeNodeId());

        NL2SqlAssistant assistant = AssistantServices.builder(NL2SqlAssistant.class)
                .chatModel(chatModel)
                .chatMemory(AppChatMemory.withMessages(historyMessages))
                .build();

        Result<String> result = assistant.generateSqlQuery(sqlDialect, databaseStructure, question);
        String sql = DatabaseUtil.cleanSql(result.content());
        String sqlResult = DatabaseUtil.executeSqlQuery(result.content(), dataSource);

        TokenUsage tokenUsage = result.tokenUsage();
        putDetails(node, Map.of(
                "question", question,
                "messageTokens", tokenUsage.inputTokenCount(),
                "answerTokens", tokenUsage.outputTokenCount()
        ));

        return buildResult(Map.of("sql", sql, "result", sqlResult));
    }
}
