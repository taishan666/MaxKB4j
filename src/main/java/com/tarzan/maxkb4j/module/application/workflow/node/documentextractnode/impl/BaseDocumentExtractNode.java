package com.tarzan.maxkb4j.module.application.workflow.node.documentextractnode.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.workflow.NodeResult;
import com.tarzan.maxkb4j.module.application.workflow.WorkflowManage;
import com.tarzan.maxkb4j.module.application.workflow.node.documentextractnode.IDocumentExtractNode;
import com.tarzan.maxkb4j.module.application.workflow.node.documentextractnode.dto.DocumentExtractParams;
import com.tarzan.maxkb4j.module.file.service.FileService;
import com.tarzan.maxkb4j.util.SpringUtil;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

public class BaseDocumentExtractNode extends IDocumentExtractNode {

    private final FileService fileService;

    public BaseDocumentExtractNode() {
        this.fileService = SpringUtil.getBean(FileService.class);
    }


    String splitter = "\n-----------------------------------\n";
    @Override
    public NodeResult execute(DocumentExtractParams nodeParams) {
        DocumentParser parser = new ApacheTikaDocumentParser();
        List<String> documentList=nodeParams.getDocumentList();
        Object res=super.getWorkflowManage().getReferenceField(documentList.get(0),documentList.subList(1,documentList.size()));
        List<Map<String,Object>> documents= (List<Map<String,Object>>) res;
        StringBuilder sb=new StringBuilder();
        for (Map<String,Object> fileMap : documents) {
            byte[] data= fileService.getBytes((String) fileMap.get("file_id"));
            Document document = parser.parse(new ByteArrayInputStream(data));
            String text = "### "+fileMap.get("name")+"\n"+document.text()+splitter;
            sb.append(text);
        }
        return new NodeResult(Map.of("content",sb.toString()),Map.of());
    }

    @Override
    public JSONObject getDetail() {
        JSONObject detail = new JSONObject();
        String content= (String) context.get("content");
        String[] fileContent= content.split(splitter);
        for (int i = 0; i < fileContent.length; i++) {
            String text = fileContent[i];
            int endIndex=Math.min(500,content.length());
            fileContent[i]= text.substring(0, endIndex);
        }
        detail.put("content",fileContent);
        return detail;
    }

    @Override
    public void saveContext(JSONObject nodeDetail, WorkflowManage workflowManage) {

    }
}
