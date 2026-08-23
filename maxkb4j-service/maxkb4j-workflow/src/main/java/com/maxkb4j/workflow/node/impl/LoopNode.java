package com.maxkb4j.workflow.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.domain.dto.Answer;
import com.maxkb4j.common.domain.dto.ChildNode;
import com.maxkb4j.workflow.annotation.NodeCreatorType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.logic.LfNode;
import com.maxkb4j.workflow.logic.LogicFlow;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.util.FormRenderUtil;
import dev.langchain4j.model.input.PromptTemplate;
import lombok.Data;

import java.util.*;

import static com.maxkb4j.workflow.consts.WorkflowConstants.*;

@NodeCreatorType(NodeType.LOOP)
public class LoopNode extends AbsNode {
    /** 循环体子节点 nodeData 中标记"输出结果"的键名 */
    private static final String KEY_IS_RESULT = NodeField.IS_RESULT;

    public LoopNode(String id, JSONObject properties) {
        super(id, properties);
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
    public List<Answer> getAnswerList() {
        Object loopData = detail.get(LoopField.LOOP_NODE_DATA);
        if (!(loopData instanceof List<?> iterations) || iterations.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> resultNodeIds = resolveResultNodeIds();
        List<Answer> answerList = new ArrayList<>();
        for (Object iteration : iterations) {
            if (iteration instanceof JSONObject iterationDetails) {
                answerList.addAll(buildIterationAnswers(iterationDetails, resultNodeIds));
            }
        }
        return answerList;
    }

    /**
     * 将单次迭代的运行时详情转换为答案列表（按节点执行顺序）。
     */
    private List<Answer> buildIterationAnswers(JSONObject iterationDetails, Set<String> resultNodeIds) {
        List<Answer> answers = new ArrayList<>();
        for (JSONObject nodeDetail : extractSortedNodeDetails(iterationDetails)) {
            answers.addAll(buildNodeAnswers(nodeDetail, resultNodeIds));
        }
        return answers;
    }

    /**
     * 从迭代详情中提取子节点详情，并按执行索引恢复节点顺序。
     * <p>JSON 反序列化不保证顺序，需依据 index 字段排序。</p>
     */
    private List<JSONObject> extractSortedNodeDetails(JSONObject iterationDetails) {
        List<JSONObject> nodeDetails = new ArrayList<>();
        for (Object value : iterationDetails.values()) {
            if (value instanceof JSONObject nodeDetail) {
                nodeDetails.add(nodeDetail);
            }
        }
        nodeDetails.sort(Comparator.comparingInt(node -> {
            Integer index = node.getInteger(RuntimeDetailField.INDEX);
            return index != null ? index : Integer.MAX_VALUE;
        }));
        return nodeDetails;
    }

    /**
     * 按子节点类型分发答案构建：表单/用户选择节点渲染交互组件，
     * 其余节点按输出节点规则提取文本答案。
     */
    private List<Answer> buildNodeAnswers(JSONObject nodeDetail, Set<String> resultNodeIds) {
        String nodeType = nodeDetail.getString(RuntimeDetailField.TYPE);
        String runtimeNodeId=super.getRuntimeNodeId();
        if (NodeType.FORM.getKey().equals(nodeType)) {
            return buildInteractiveAnswers(runtimeNodeId,nodeDetail, FormField.FORM_RENDER_TAG, true);
        }
        if (NodeType.USER_SELECT.getKey().equals(nodeType)) {
            return buildInteractiveAnswers(runtimeNodeId,nodeDetail, FormField.CARD_SELECTION_RENDER_TAG, false);
        }
        if (!isResultNode(nodeDetail, resultNodeIds)) {
            return Collections.emptyList();
        }
        return buildTextAnswer(runtimeNodeId,nodeDetail);
    }

    /**
     * 构建输出节点的文本答案。
     */
    private List<Answer> buildTextAnswer(String runtimeNodeId,JSONObject nodeDetail) {
        Object content = nodeDetail.get(NodeField.ANSWER);
        if (content == null) {
            return Collections.emptyList();
        }
        Object reasoningContent = nodeDetail.get(NodeField.REASONING_CONTENT);
        return List.of(Answer.builder()
                .content(String.valueOf(content))
                .reasoningContent(reasoningContent != null ? String.valueOf(reasoningContent) : "")
                .chatRecordId("")
                .runtimeNodeId(runtimeNodeId)
                .viewType(ViewType.MANY_VIEW)
                .build());
    }

    /**
     * 构建表单/用户选择节点的交互组件答案：以渲染标签包裹表单设置，可选套用内容模板。
     *
     * @param renderTag          渲染标签（form_render / card_selection_render）
     * @param applyContentFormat 是否套用 form_content_format 模板
     */
    private List<Answer> buildInteractiveAnswers(String runtimeNodeId,JSONObject nodeDetail,
                                                 String renderTag, boolean applyContentFormat) {
        String chatRecordId = nodeDetail.getString(ChatField.CHAT_RECORD_ID);
        String childRuntimeNodeId = nodeDetail.getString(RuntimeDetailField.RUNTIME_NODE_ID);
        String formRender = FormRenderUtil.buildFormRender(nodeDetail, renderTag);
        String content = formRender;
        if (applyContentFormat) {
            String formContentFormat = nodeDetail.getString(FormField.FORM_CONTENT_FORMAT);
            if (formContentFormat != null) {
                content = PromptTemplate.from(formContentFormat).apply(Map.of(FormField.FORM, formRender)).text();
            }
        }
        return List.of(Answer.builder()
                .content(content)
                .reasoningContent("")
                .chatRecordId(chatRecordId)
                .runtimeNodeId(runtimeNodeId)
                .childNode(new ChildNode(chatRecordId,childRuntimeNodeId))
                .viewType(ViewType.SINGLE_VIEW)
                .build());
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
            if (nodeData != null && Boolean.TRUE.equals(nodeData.getBoolean(KEY_IS_RESULT))) {
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
