package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import com.tarzan.maxkb4j.common.util.DatabaseUtil;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.INode;
import com.tarzan.maxkb4j.core.workflow.node.impl.NL2SqlNode;
import com.tarzan.maxkb4j.core.workflow.result.NodeResult;
import com.tarzan.maxkb4j.module.model.info.service.ModelFactory;
import dev.langchain4j.experimental.rag.content.retriever.sql.SqlDatabaseContentRetriever;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.query.Query;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

@Slf4j
@AllArgsConstructor
@Component
public class NL2SqlNodeHandler implements INodeHandler {

    private final ModelFactory modelFactory;
    @Override
    public NodeResult execute(Workflow workflow, INode node) throws Exception {
        NL2SqlNode.NodeParams nodeParams=node.getNodeData().toJavaObject(NL2SqlNode.NodeParams.class);
        NL2SqlNode.DatabaseSetting databaseSetting=nodeParams.getDatabaseSetting();
        ChatModel chatModel = modelFactory.buildChatModel(nodeParams.getModelId(), nodeParams.getModelParamsSetting());
        DataSource dataSource = DatabaseUtil.getDataSource(databaseSetting.getType(), databaseSetting.getHost(), databaseSetting.getPort(), databaseSetting.getUsername(), databaseSetting.getPassword(), databaseSetting.getDatabase());
        SqlDatabaseContentRetriever sqlDatabaseContentRetriever = SqlDatabaseContentRetriever.builder().chatModel(chatModel).dataSource(dataSource).maxRetries(2).build();
        List<String> fields=nodeParams.getQuestionReferenceAddress();
        String question= (String)workflow.getReferenceField(fields.get(0),fields.get(1));
        List<Content> contents = sqlDatabaseContentRetriever.retrieve(Query.from(question));
        Content content = contents.get(0);
        String text=content.textSegment().text();
        String sql=text.substring(text.indexOf("'")+1,text.lastIndexOf("':"));
        String result=text.split(":")[1];
        System.out.println(content);
        System.out.println("sql= "+sql);
        return new NodeResult(Map.of("sql",sql,"result",result), Map.of());
    }
}
