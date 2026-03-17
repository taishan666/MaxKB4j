package com.maxkb4j.workflow.handler.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.common.domain.dto.ChatMessageVO;
import com.maxkb4j.common.domain.dto.ChildNode;
import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.builder.NodeBuilder;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.INodeHandler;
import com.maxkb4j.workflow.logic.LogicFlow;
import com.maxkb4j.workflow.model.LoopParams;
import com.maxkb4j.workflow.model.LoopWorkFlow;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.LoopNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;
import service.IWorkFlowActuator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.maxkb4j.workflow.enums.NodeType.*;

@Slf4j
@NodeHandlerType(NodeType.LOOP)
@Component
@RequiredArgsConstructor
public class LoopNodeHandler implements INodeHandler {

    private final IWorkFlowActuator workFlowActuator;

    @SuppressWarnings("unchecked")
    @Override
    public NodeResult execute(Workflow workflow, AbsNode node) throws Exception {
        LoopNode.NodeParams nodeParams = node.getNodeData().toJavaObject(LoopNode.NodeParams.class);
        String loopType = nodeParams.getLoopType();
        List<String> array = nodeParams.getArray();
        Integer number = nodeParams.getNumber();
        JSONObject loopBody = nodeParams.getLoopBody();
        List<JSONObject> loopDetails = new ArrayList<>();

        if ("ARRAY".equals(loopType)) {
            Object value = workflow.getReferenceField(array);
            if (value != null) {
                List<Object> list = parseLoopArray(value);
                loopDetails = generateLoopArray(list, workflow, loopBody, node);
            }
        } else if ("LOOP".equals(loopType)) {
            loopDetails = generateLoopNumber(1000, workflow, loopBody, node);
        } else {
            loopDetails = generateLoopNumber(number, workflow, loopBody, node);
        }

        node.getDetail().put("loop_node_data", loopDetails);
        node.getDetail().put("loopType", loopType);
        node.getDetail().put("number", number);
        return new NodeResult(Map.of(), true, this::isInterrupt);
    }

    /**
     * Check if the node execution should be interrupted.
     */
    public boolean isInterrupt(AbsNode node) {
        Object interruptFlag = node.getDetail().get("is_interrupt_exec");
        return interruptFlag != null && (boolean) interruptFlag;
    }

    /**
     * Parse loop array from various input types.
     *
     * @param value the input value (List, String, or other)
     * @return parsed list of objects
     */
    @SuppressWarnings("unchecked")
    private List<Object> parseLoopArray(Object value) {
        if (value instanceof List<?>) {
            return (List<Object>) value;
        }
        if (value instanceof String) {
            Gson gson = new Gson();
            String inputStr = ((String) value).trim();
            if (inputStr.startsWith("[") && inputStr.endsWith("]")) {
                return gson.fromJson(inputStr, new TypeToken<List<Object>>() {}.getType());
            }
            return List.of(value);
        }
        return List.of(value);
    }

    private List<JSONObject> generateLoopArray(List<Object> array, Workflow workflow, JSONObject loopBody, AbsNode node) {
        return generateLoop(array, workflow, loopBody, node);
    }

    private List<JSONObject> generateLoopNumber(int number, Workflow workflow, JSONObject loopBody, AbsNode node) {
        List<Object> array = new ArrayList<>();
        for (int i = 0; i < number; i++) {
            array.add(i);
        }
        return generateLoop(array, workflow, loopBody, node);
    }

    /**
     * Main loop execution method.
     */
    @SuppressWarnings("unchecked")
    private List<JSONObject> generateLoop(List<Object> list, Workflow workflow, JSONObject loopBody, AbsNode node) {
        AtomicBoolean breakOuter = new AtomicBoolean(false);
        List<JSONObject> loopNodeData = (List<JSONObject>) node.getDetail().get("loop_node_data");
        List<JSONObject> loopDetails = loopNodeData == null ? new ArrayList<>() : loopNodeData;
        Object currentIndex = node.getDetail().get("current_index");
        int startIndex = currentIndex == null ? 0 : (int) currentIndex;

        ChatParams chatParams = workflow.getChatParams();
        if (chatParams.getChildNode() != null) {
            chatParams.setRuntimeNodeId(chatParams.getChildNode().getRuntimeNodeId());
        }

        JSONObject details = new JSONObject();
        if (loopDetails.size() > startIndex) {
            details = loopDetails.get(startIndex);
        }

        AtomicBoolean isInterruptExec = new AtomicBoolean(false);

        do {
            if (loopDetails.size() > startIndex) {
                loopDetails.remove(0);
            }

            // Execute single loop iteration
            LoopIterationResult iterationResult = executeLoopIteration(
                    workflow, loopBody, node, list, startIndex, details, breakOuter, isInterruptExec);

            // Process iteration results
            loopDetails.add(iterationResult.runtimeDetails);
            node.getDetail().put("is_interrupt_exec", iterationResult.isInterrupt);
            node.getDetail().put("current_index", startIndex);

            if (shouldBreakLoop(breakOuter.get(), iterationResult.isInterrupt)) {
                break;
            }

            chatParams.setRuntimeNodeId(null);
            startIndex++;
        } while (startIndex < list.size());

        return loopDetails;
    }

    /**
     * Execute a single loop iteration.
     */
    private LoopIterationResult executeLoopIteration(
            Workflow workflow, JSONObject loopBody, AbsNode node,
            List<Object> list, int startIndex, JSONObject details,
            AtomicBoolean breakOuter, AtomicBoolean isInterruptExec) {

        Sinks.Many<ChatMessageVO> nodeSink = Sinks.many().unicast().onBackpressureBuffer();
        LogicFlow logicFlow = LogicFlow.newInstance(loopBody);
        List<AbsNode> nodes = logicFlow.getNodes().stream()
                .map(NodeBuilder::getNode)
                .filter(Objects::nonNull)
                .toList();

        LoopParams loopParams = new LoopParams(startIndex, list.get(startIndex));
        LoopWorkFlow loopWorkflow = createLoopWorkflow(workflow, nodes, logicFlow, loopParams, details, nodeSink);

        // Execute asynchronously
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> workFlowActuator.execute(workflow));

        AtomicReference<ChildNode> childNode = new AtomicReference<>(null);

        // Subscribe and accumulate tokens, send messages
        nodeSink.asFlux().subscribe(e -> handleLoopMessage(e, loopParams, node, workflow, childNode, breakOuter, isInterruptExec));

        // Send end message
        sendLoopEndMessage(node, workflow, childNode);

        future.join();

        // Process runtime details
        JSONObject runtimeDetails = processRuntimeDetails(loopWorkflow, startIndex);

        return new LoopIterationResult(runtimeDetails, isInterruptExec.get());
    }

    /**
     * Create a loop workflow instance.
     */
    private LoopWorkFlow createLoopWorkflow(
            Workflow workflow, List<AbsNode> nodes, LogicFlow logicFlow,
            LoopParams loopParams, JSONObject details, Sinks.Many<ChatMessageVO> nodeSink) {
        return new LoopWorkFlow(workflow, nodes, logicFlow.getEdges(), loopParams, details, nodeSink);
    }

    /**
     * Handle a single message from the loop execution.
     */
    private void handleLoopMessage(
            ChatMessageVO e, LoopParams loopParams, AbsNode node, Workflow workflow,
            AtomicReference<ChildNode> childNode, AtomicBoolean breakOuter, AtomicBoolean isInterruptExec) {

        if (LOOP_BREAK.getKey().equals(e.getNodeType()) && "BREAK".equals(e.getContent())) {
            breakOuter.set(true);
        } else {
            if (FORM.getKey().equals(e.getNodeType()) || USER_SELECT.getKey().equals(e.getNodeType())) {
                breakOuter.set(true);
                isInterruptExec.set(true);
            }

            String runtimeNodeId = e.getRuntimeNodeId() + "_" + loopParams.getIndex();
            childNode.set(new ChildNode(e.getChatRecordId(), runtimeNodeId));

            ChatMessageVO vo = node.toChatMessageVO(
                    workflow.getChatParams().getChatId(),
                    workflow.getChatParams().getChatRecordId(),
                    e.getContent(),
                    e.getReasoningContent(),
                    childNode.get(),
                    false);
            vo.setNodeType(e.getNodeType());
            vo.setViewType(e.getViewType());

            if (workflow.needsSinkOutput()) {
                workflow.getSink().tryEmitNext(vo);
            }
        }
    }

    /**
     * Send the loop iteration end message.
     */
    private void sendLoopEndMessage(AbsNode node, Workflow workflow, AtomicReference<ChildNode> childNode) {
        ChatMessageVO vo = node.toChatMessageVO(
                workflow.getChatParams().getChatId(),
                workflow.getChatParams().getChatRecordId(),
                "",
                "",
                childNode.get(),
                false);

        if (workflow.needsSinkOutput()) {
            workflow.getSink().tryEmitNext(vo);
        }
    }

    /**
     * Process runtime details and update node IDs with loop index.
     */
    private JSONObject processRuntimeDetails(LoopWorkFlow loopWorkflow, int startIndex) {
        JSONObject runtimeDetails = loopWorkflow.getRuntimeDetails();
        for (String key : runtimeDetails.keySet()) {
            JSONObject value = runtimeDetails.getJSONObject(key);
            String runtimeNodeId = value.getString("runtimeNodeId");
            value.put("runtimeNodeId", runtimeNodeId + "_" + startIndex);
        }
        return runtimeDetails;
    }

    /**
     * Determine if the loop should break.
     */
    private boolean shouldBreakLoop(boolean breakOuter, boolean isInterrupt) {
        return breakOuter || isInterrupt;
    }

    /**
     * Record class for loop iteration results.
     */
    private record LoopIterationResult(JSONObject runtimeDetails, boolean isInterrupt) {
    }
}