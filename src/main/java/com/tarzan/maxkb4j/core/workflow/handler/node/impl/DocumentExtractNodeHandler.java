package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.model.ChatFile;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.INode;
import com.tarzan.maxkb4j.core.workflow.node.impl.DocumentExtractNode;
import com.tarzan.maxkb4j.core.workflow.result.NodeResult;
import com.tarzan.maxkb4j.module.knowledge.service.DocumentParseService;
import com.tarzan.maxkb4j.module.oss.service.MongoFileService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
@Component
public class DocumentExtractNodeHandler implements INodeHandler {

    private final MongoFileService fileService;
    private final DocumentParseService documentParseService;

    @SuppressWarnings("unchecked")
    @Override
    public NodeResult execute(Workflow workflow, INode node) throws Exception {
        String splitter = "\n-----------------------------------\n";
        DocumentExtractNode.NodeParams nodeParams=node.getNodeData().toJavaObject(DocumentExtractNode.NodeParams.class);
        List<String> documentList=nodeParams.getDocumentList();
        Object res=workflow.getReferenceField(documentList.get(0),documentList.get(1));
        List<String> content=new LinkedList<>();
        List<ChatFile> documents=new LinkedList<>();
        if (res!=null){
            if (res instanceof List){
                documents= (List<ChatFile>) res;
                for (ChatFile chatFile : documents) {
                    InputStream data= fileService.getStream(chatFile.getFileId());
                    String extractText=documentParseService.extractText(data);
                    String text = "### "+chatFile.getName()+"\n"+extractText+splitter;
                    content.add(text);
                }
            }
        }
        return new NodeResult(Map.of("content",String.join(splitter, content),"documentList",documents));
    }
}
