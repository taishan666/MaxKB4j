package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import com.tarzan.maxkb4j.common.util.DatabaseUtil;
import com.tarzan.maxkb4j.core.assistant.NL2SqlAssistant;
import com.tarzan.maxkb4j.core.langchain4j.AppChatMemory;
import com.tarzan.maxkb4j.core.workflow.annotation.NodeHandlerType;
import com.tarzan.maxkb4j.core.workflow.enums.NodeType;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.AbsNode;
import com.tarzan.maxkb4j.core.workflow.node.impl.NL2SqlNode;
import com.tarzan.maxkb4j.core.workflow.model.NodeResult;
import com.tarzan.maxkb4j.module.model.info.factory.impl.ModelFactory;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.AiServices;
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
public class NL2SqlNodeHandler implements INodeHandler {

    private final ModelFactory modelFactory;
    @Override
    public NodeResult execute(Workflow workflow, AbsNode node) throws Exception {
        NL2SqlNode.NodeParams nodeParams=node.getNodeData().toJavaObject(NL2SqlNode.NodeParams.class);
        NL2SqlNode.DatabaseSetting databaseSetting=nodeParams.getDatabaseSetting();
        List<String> fields=nodeParams.getQuestionReferenceAddress();
        String question= (String)workflow.getReferenceField(fields);
        ChatModel chatModel = modelFactory.buildChatModel(nodeParams.getModelId(), nodeParams.getModelParamsSetting());
        DataSource dataSource = DatabaseUtil.getDataSource(databaseSetting.getType(), databaseSetting.getHost(), databaseSetting.getPort(), databaseSetting.getUsername(), databaseSetting.getPassword(), databaseSetting.getDatabase());
        String sqlDialect=DatabaseUtil.getSqlDialect(dataSource);
        String databaseStructure=DatabaseUtil.generateDDL(dataSource);
        List<ChatMessage> historyMessages = workflow.getHistoryMessages(nodeParams.getDialogueNumber(), nodeParams.getDialogueType(), node.getRuntimeNodeId());
        NL2SqlAssistant  assistant= AiServices.builder(NL2SqlAssistant.class).chatModel(chatModel).chatMemory(AppChatMemory.withMessages(historyMessages)).build();
        Result<String> result=assistant.generateSqlQuery(sqlDialect,databaseStructure,question);
        String sql=DatabaseUtil.cleanSql(result.content());
        String sqlResult=DatabaseUtil.executeSqlQuery(result.content(),dataSource);
        TokenUsage tokenUsage =  result.tokenUsage();
        node.getDetail().put("question", question);
        node.getDetail().put("messageTokens", tokenUsage.inputTokenCount());
        node.getDetail().put("answerTokens", tokenUsage.outputTokenCount());
        return new NodeResult(Map.of("sql",sql,"result",sqlResult));
    }

}
