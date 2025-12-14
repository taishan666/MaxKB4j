package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import com.tarzan.maxkb4j.common.util.DatabaseUtil;
import com.tarzan.maxkb4j.core.assistant.NL2SqlAssistant;
import com.tarzan.maxkb4j.core.langchain4j.AppChatMemory;
import com.tarzan.maxkb4j.core.workflow.annotation.NodeHandlerType;
import com.tarzan.maxkb4j.core.workflow.enums.NodeType;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.INode;
import com.tarzan.maxkb4j.core.workflow.node.impl.NL2SqlNode;
import com.tarzan.maxkb4j.core.workflow.model.NodeResult;
import com.tarzan.maxkb4j.module.model.info.service.ModelFactory;
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
    public NodeResult execute(Workflow workflow, INode node) throws Exception {
        NL2SqlNode.NodeParams nodeParams=node.getNodeData().toJavaObject(NL2SqlNode.NodeParams.class);
        NL2SqlNode.DatabaseSetting databaseSetting=nodeParams.getDatabaseSetting();
        List<String> fields=nodeParams.getQuestionReferenceAddress();
        String question= (String)workflow.getReferenceField(fields.get(0),fields.get(1));
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

/*    public static String convertToMarkdown(String result) {
        String[] rows=result.split("\n");
        StringBuilder markdown = new StringBuilder();
        String topRow = rows[0];
        List<String> HEADERS=Arrays.stream(topRow.split(",")).map(String::trim).toList();
        // 添加表头
        markdown.append("| ").append(String.join(" | ", HEADERS)).append(" |\n");

        // 添加分隔行
        markdown.append("|").append(HEADERS.stream().map(h -> " -- ").collect(Collectors.joining("|"))).append("|\n");

        for (int i = 1; i < rows.length; i++) {
            String line=rows[i];
            String[] fields = parseCsvLine(line);
            if (fields.length < HEADERS.size()) {
                // 如果字段不足，补空字符串
                fields = Arrays.copyOf(fields, HEADERS.size());
            }

            markdown.append("| ")
                    .append(Arrays.stream(fields)
                            .limit(HEADERS.size())
                            .collect(Collectors.joining(" | ")))
                    .append(" |\n");
        }
        return markdown.toString();
    }

    // 简单 CSV 解析（不处理引号内逗号，适用于你当前的数据）
    private static String[] parseCsvLine(String line) {
        // 去掉末尾可能多余的逗号（如你数据最后一列后还有逗号）
        if (line.endsWith(",")) {
            line = line.substring(0, line.length() - 1);
        }
        return line.split(",", -1); // -1 保留尾部空字段
    }*/



}
