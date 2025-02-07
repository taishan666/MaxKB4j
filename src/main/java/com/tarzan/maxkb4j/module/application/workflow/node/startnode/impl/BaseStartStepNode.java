package com.tarzan.maxkb4j.module.application.workflow.node.startnode.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.workflow.Node;
import com.tarzan.maxkb4j.module.application.workflow.NodeResult;
import com.tarzan.maxkb4j.module.application.workflow.WorkflowManage;
import com.tarzan.maxkb4j.module.application.workflow.dto.FlowParams;
import com.tarzan.maxkb4j.module.application.workflow.node.NodeDetail;
import com.tarzan.maxkb4j.module.application.workflow.node.startnode.IStarNode;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class BaseStartStepNode extends IStarNode {
    @Override
    public NodeResult execute(FlowParams workflowParams, WorkflowManage workflowManage) {
        // 获取基础节点
        Node baseNode = workflowManage.getBaseNode();
        // 获取默认全局变量
        List<JSONObject> inputFieldList = (List<JSONObject>) baseNode.getProperties()
                .getOrDefault("input_field_list", Collections.emptyList());
        JSONObject defaultGlobalVariable = getDefaultGlobalVariable(inputFieldList);
        // 合并全局变量
        Map<String, Object> workflowVariable = new HashMap<>(defaultGlobalVariable);
        workflowVariable.putAll(getGlobalVariable(workflowParams, workflowManage));
        // 构建节点变量
        Map<String, Object> nodeVariable = new HashMap<>();
        nodeVariable.put("question", workflowParams.getQuestion());
        nodeVariable.put("image", workflowManage.getImageList());
        nodeVariable.put("document", workflowManage.getDocumentList());
        nodeVariable.put("audio", workflowManage.getAudioList());

        return new NodeResult(nodeVariable, workflowVariable);
    }

    public Map<String, Object> getGlobalVariable(FlowParams workflowParams, WorkflowManage workflowManage) {
        // 获取历史聊天记录
        List<ApplicationChatRecordEntity> historyChatRecord = workflowParams.getHistoryChatRecord();
        List<Map<String, String>> historyContext = new ArrayList<>();

        for (ApplicationChatRecordEntity chatRecord : historyChatRecord) {
            Map<String, String> record = new HashMap<>();
            record.put("question", chatRecord.getProblemText());
            record.put("answer", chatRecord.getAnswerText());
            historyContext.add(record);
        }

        // 获取chat_id并确保其为字符串形式
        String chatId = workflowParams.getChatId();

        // 构建返回的map
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        resultMap.put("start_time", System.currentTimeMillis() / 1000.0); // 转换为秒级时间戳
        resultMap.put("history_context", historyContext);
        resultMap.put("chat_id", chatId);

        // 合并node.workflow_manage.form_data
        if (workflowManage != null && workflowManage.getFormData() != null) {
            resultMap.putAll(workflowManage.getFormData());
        }

        return resultMap;
    }


    @Override
    public void saveContext(NodeDetail nodeDetail, WorkflowManage workflowManage) {

    }
}
