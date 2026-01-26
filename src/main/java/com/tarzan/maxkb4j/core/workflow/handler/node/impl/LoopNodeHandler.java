package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.common.util.SpringUtil;
import com.tarzan.maxkb4j.core.workflow.annotation.NodeHandlerType;
import com.tarzan.maxkb4j.core.workflow.builder.NodeBuilder;
import com.tarzan.maxkb4j.core.workflow.enums.NodeType;
import com.tarzan.maxkb4j.core.workflow.enums.WorkflowMode;
import com.tarzan.maxkb4j.core.workflow.handler.WorkflowHandler;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.logic.LogicFlow;
import com.tarzan.maxkb4j.core.workflow.model.NodeResult;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.AbsNode;
import com.tarzan.maxkb4j.core.workflow.node.impl.LoopNode;
import com.tarzan.maxkb4j.module.chat.dto.ChatParams;
import org.springframework.stereotype.Component;

import java.util.*;

@NodeHandlerType(NodeType.LOOP_NODE)
@Component
public class LoopNodeHandler implements INodeHandler {

    private static ChatParams chatParams = null;
    private static AbsNode currentNode;

    @Override
    public NodeResult execute(Workflow workflow, AbsNode node) throws Exception {
        Map<String, Object> nodeVariable = new HashMap<>();
        LoopNode.NodeParams nodeParams = node.getNodeData().toJavaObject(LoopNode.NodeParams.class);
        chatParams = workflow.getChatParams();
        currentNode = workflow.getCurrentNode();

        String loopType = nodeParams.getLoopType();
        List<Object> array = nodeParams.getArray();
        Integer number = nodeParams.getNumber();
        JSONObject loopBody = nodeParams.getLoopBody();

        if (loopType == null && node.getProperties() != null) {
            loopType = node.getProperties().getJSONObject("node_data").getString("loop_type");
        }
        if (array == null && node.getProperties() != null) {
            array = node.getProperties().getJSONObject("node_data").getObject("array", List.class);
        }
        if (number == null && node.getProperties() != null) {
            number = node.getProperties().getJSONObject("node_data").getInteger("number");
        }
        if (loopBody == null && node.getProperties() != null) {
            loopBody = node.getProperties().getJSONObject("node_data").getJSONObject("loop_body");
        }

        if ("ARRAY".equals(loopType) && array != null && !array.isEmpty()) {
            if (array.size() > 1) {
                Object value = workflow.getReferenceField(array.get(0).toString(), array.get(1).toString());
                nodeVariable.put("array", JSONUtil.parseArray(value).toList(Object.class));
            } else {
                nodeVariable.put("array", array);
            }
        }

        nodeVariable.put("loopType", loopType);
        nodeVariable.put("number", number);
        nodeVariable.put("loopBody", loopBody);
        nodeVariable.put("loop_node", true);

        NodeResult nodeResult = new NodeResult(nodeVariable);
        nodeResult.setWriteContextFunc((nv, n, wf) -> {
            try {
                writeContext(nv, n, wf, node.getProperties().getJSONObject("node_data"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return nodeResult;
    }

    public boolean isInterrupt(AbsNode node) {
        return node.getContext().getOrDefault("is_interrupt_exec", false).equals(true);
    }

    private void writeContext(Map<String, Object> nodeVariable, AbsNode node, Workflow workflow, JSONObject loopBody) throws Exception {

        String loopType = (String) nodeVariable.get("loopType");
        ArrayList<Object> array = (ArrayList<Object>) nodeVariable.get("array");
        Integer number = (Integer) nodeVariable.get("number");

        WorkflowManageNewInstance workflowManageNewInstance = new WorkflowManageNewInstance(workflow, loopBody);

        if ("ARRAY".equals(loopType)) {
            generateLoopArray(array, workflowManageNewInstance, node);
        } else if ("LOOP".equals(loopType)) {
            generateWhileLoop(workflowManageNewInstance, node);
        } else {
            generateLoopNumber(number, workflowManageNewInstance, node);
        }

    }

    private void generateLoopArray(List<Object> array, WorkflowManageNewInstance workflowManageNewInstance, AbsNode node) throws Exception {
        int startIndex = (int) node.getContext().getOrDefault("current_index", 0);

        for (int i = startIndex; i < array.size(); i++) {
            Object item = array.get(i);
            workflowManageNewInstance.createAndRunWorkflow(Map.of("index", i, "item", item));
        }
    }

    private void generateLoopNumber(Integer number, WorkflowManageNewInstance workflowManageNewInstance, AbsNode node) throws Exception {
        int startIndex = (int) node.getContext().getOrDefault("current_index", 0);
        for (int i = startIndex; i < number; i++) {
            workflowManageNewInstance.createAndRunWorkflow(Map.of("index", i, "item", i));
        }
    }

    private void generateWhileLoop(WorkflowManageNewInstance workflowManageNewInstance, AbsNode node) throws Exception {
        int startIndex = (int) node.getContext().getOrDefault("current_index", 0);
        int i = startIndex;
        while (true) {
            workflowManageNewInstance.createAndRunWorkflow(Map.of("index", i, "item", i));
            i++;
        }
    }

    private static class WorkflowManageNewInstance {
        private final Workflow parentWorkflow;
        private final JSONObject loopBody;

        public WorkflowManageNewInstance(Workflow parentWorkflow, JSONObject loopBody) {
            this.parentWorkflow = parentWorkflow;
            this.loopBody = loopBody;
        }

        public void createAndRunWorkflow(Map<String, Object> loopData) throws Exception {
            LogicFlow logicFlow = LogicFlow.newInstance(loopBody.getJSONObject("loop_body"));
            List<AbsNode> nodes = logicFlow.getNodes().stream().map(NodeBuilder::getNode).filter(Objects::nonNull).toList();

            Workflow loopWorkflow = new Workflow(WorkflowMode.APPLICATION, nodes, logicFlow.getEdges());
            loopWorkflow.setCurrentNode(currentNode);
            loopWorkflow.setChatParams(chatParams);
            loopWorkflow.getContext().putAll(loopData);
            WorkflowHandler workflowHandler = SpringUtil.getBean("workflowHandler", WorkflowHandler.class);
            workflowHandler.runChainNodes(loopWorkflow, nodes);

        }
    }
}
