package com.maxkb4j.workflow.node.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.domain.dto.Answer;
import com.maxkb4j.workflow.annotation.NodeCreatorType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.logic.LfNode;
import com.maxkb4j.workflow.logic.LogicFlow;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.node.AbsNode;
import dev.langchain4j.model.input.PromptTemplate;
import lombok.Data;

import java.util.*;

import static com.maxkb4j.workflow.consts.WorkflowConstants.*;

@NodeCreatorType(NodeType.LOOP)
public class LoopNode extends AbsNode {
    public LoopNode(String id, JSONObject properties) {
        super(id,properties);
    }

    @Override
    public void saveContext(IWorkflow workflow, Map<String, Object> detail) {
        context.put(LoopField.CURRENT_INDEX, detail.get(LoopField.CURRENT_INDEX));
    }

    /**
     * 聚合循环体子节点历次迭代产生的答案。
     * <p>
     * 迭代详情由 LoopIterationRunner 写入 {@code detail} 的 {@code loop_node_data}：
     * 外层为迭代列表，内层以子节点 runtimeNodeId 为键；值中的 runtimeNodeId 已追加
     * "_迭代下标" 后缀，与 LoopMessageForwarder 流式输出的 ChildNode 一致，
     * 前端据此将持久化答案与循环内流式消息对齐。
     */
    @Override
    public List<Answer> getAnswerList(String chatRecordId) {
        Object loopData = detail.get(LoopField.LOOP_NODE_DATA);
        if (!(loopData instanceof List<?> iterations) || iterations.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> resultNodeIds = resolveResultNodeIds();
        List<Answer> answerList = new ArrayList<>();
        for (Object iteration : iterations) {
            if (iteration instanceof JSONObject iterationDetails) {
                answerList.addAll(buildIterationAnswers(iterationDetails, resultNodeIds, chatRecordId));
            }
        }
        return answerList;
    }

    /**
     * 将单次迭代的运行时详情转换为答案列表（按节点执行顺序）。
     */
    private List<Answer> buildIterationAnswers(JSONObject iterationDetails, Set<String> resultNodeIds,
                                               String chatRecordId) {
        List<JSONObject> nodeDetails = new ArrayList<>();
        for (Object value : iterationDetails.values()) {
            if (value instanceof JSONObject nodeDetail) {
                nodeDetails.add(nodeDetail);
            }
        }
        // JSON 反序列化不保证顺序，按执行索引恢复节点顺序
        nodeDetails.sort(Comparator.comparingInt(node -> {
            Integer index = node.getInteger(RuntimeDetailField.INDEX);
            return index != null ? index : Integer.MAX_VALUE;
        }));

        List<Answer> answers = new ArrayList<>(nodeDetails.size());
        for (JSONObject nodeDetail : nodeDetails) {
            String nodeType=nodeDetail.getString("type");
            if(NodeType.FORM.getKey().equals(nodeType)){
                answers.addAll(getFormAnswerList(chatRecordId,nodeDetail));
                continue;
            }
            if(NodeType.USER_SELECT.getKey().equals(nodeType)){
                answers.addAll(getUserSelectAnswerList(chatRecordId,nodeDetail));
                continue;
            }
            if (!isResultNode(nodeDetail, resultNodeIds)) {
                continue;
            }
            Object content = nodeDetail.get(NodeField.ANSWER);
            if (content == null) {
                continue;
            }
            Object reasoningContent = nodeDetail.get(NodeField.REASONING_CONTENT);
            answers.add(Answer.builder()
                    .content(String.valueOf(content))
                    .reasoningContent(reasoningContent != null ? String.valueOf(reasoningContent) : "")
                    .chatRecordId(chatRecordId)
                    .runtimeNodeId(nodeDetail.getString(RuntimeDetailField.RUNTIME_NODE_ID))
                    .realNodeId(nodeDetail.getString(RuntimeDetailField.RUNTIME_NODE_ID))
                    .viewType(ViewType.MANY_VIEW)
                    .build());
        }
        return answers;
    }

    public List<Answer> getFormAnswerList(String chatRecordId,JSONObject nodeDetail) {
        JSONObject formData =nodeDetail.getJSONObject(FormField.FORM_DATA);
        boolean isSubmit = nodeDetail.getBooleanValue(FormField.IS_SUBMIT);
        String runtimeNodeId = nodeDetail.getString(RuntimeDetailField.RUNTIME_NODE_ID);
        JSONArray formFieldList = nodeDetail.getJSONArray(FormField.FORM_FIELD_LIST);
        JSONObject formSetting = new JSONObject();
        formSetting.put(FormField.FORM_FIELD_LIST, formFieldList);
        formSetting.put(FormField.IS_SUBMIT, isSubmit);
        formSetting.put(FormField.FORM_DATA, formData);
        formSetting.put(RuntimeDetailField.RUNTIME_NODE_ID, runtimeNodeId);
        formSetting.put(ChatField.CHAT_RECORD_ID, chatRecordId);
        String formRender = "<" + FormField.FORM_RENDER_TAG + ">" + formSetting + "</" + FormField.FORM_RENDER_TAG + ">";
        String formContentFormat = nodeDetail.getString(FormField.FORM_CONTENT_FORMAT);
        if (formContentFormat != null) {
            PromptTemplate promptTemplate = PromptTemplate.from(formContentFormat);
            String answer = promptTemplate.apply(Map.of("form", formRender)).text();
            return List.of(Answer.builder().content(answer).reasoningContent("").chatRecordId(chatRecordId).runtimeNodeId(runtimeNodeId).realNodeId(runtimeNodeId).viewType(ViewType.SINGLE_VIEW).build());
        }
        return List.of(Answer.builder().content(formRender).reasoningContent("").chatRecordId(chatRecordId).runtimeNodeId(runtimeNodeId).realNodeId(runtimeNodeId).viewType(ViewType.SINGLE_VIEW).build());
    }

    public List<Answer> getUserSelectAnswerList(String chatRecordId,JSONObject nodeDetail) {
        JSONObject formData =nodeDetail.getJSONObject(FormField.FORM_DATA);
        boolean isSubmit = nodeDetail.getBooleanValue(FormField.IS_SUBMIT);
        String runtimeNodeId = nodeDetail.getString(RuntimeDetailField.RUNTIME_NODE_ID);
        JSONArray formFieldList = nodeDetail.getJSONArray(FormField.FORM_FIELD_LIST);
        JSONObject formSetting = new JSONObject();
        formSetting.put(FormField.FORM_FIELD_LIST, formFieldList);
        formSetting.put(FormField.IS_SUBMIT, isSubmit);
        formSetting.put(FormField.FORM_DATA, formData);
        formSetting.put(RuntimeDetailField.RUNTIME_NODE_ID, runtimeNodeId);
        formSetting.put(ChatField.CHAT_RECORD_ID, chatRecordId);
        String formRender = "<" + FormField.CARD_SELECTION_RENDER_TAG + ">" + formSetting + "</" + FormField.CARD_SELECTION_RENDER_TAG + ">";
        return List.of(Answer.builder().content(formRender).reasoningContent("").chatRecordId(chatRecordId).runtimeNodeId(runtimeNodeId).realNodeId(runtimeNodeId).viewType(ViewType.SINGLE_VIEW).build());
    }


    /**
     * 判断子节点是否为输出节点；循环体不可解析时退化为"详情中带有答案即输出"。
     */
    private boolean isResultNode(JSONObject nodeDetail, Set<String> resultNodeIds) {
        if (resultNodeIds == null) {
            return nodeDetail.containsKey(NodeField.ANSWER);
        }
        return resultNodeIds.contains(nodeDetail.getString(RuntimeDetailField.NODE_ID));
    }

    /**
     * 从循环体（loopBody）解析标记为"输出结果"的子节点 ID 集合。
     *
     * @return 结果节点 ID 集合；循环体不可解析时返回 null（走退化逻辑）
     */
    private Set<String> resolveResultNodeIds() {
        NodeParams params = getNodeData().toJavaObject(NodeParams.class);
        JSONObject loopBody = params != null ? params.getLoopBody() : null;
        if (loopBody == null) {
            return null;
        }
        List<LfNode> nodes = LogicFlow.newInstance(loopBody).getNodes();
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }
        Set<String> resultNodeIds = new HashSet<>();
        for (LfNode node : nodes) {
            JSONObject properties = node.getProperties();
            JSONObject nodeData = properties != null ? properties.getJSONObject(RuntimeDetailField.NODE_DATA) : null;
            if (nodeData != null && Boolean.TRUE.equals(nodeData.getBoolean("isResult"))) {
                resultNodeIds.add(node.getId());
            }
        }
        return resultNodeIds;
    }

    @Data
    public static class NodeParams {
        private String loopType;
        private JSONObject loopBody;
        private Integer number;
        private List<String> array;
    }
}
