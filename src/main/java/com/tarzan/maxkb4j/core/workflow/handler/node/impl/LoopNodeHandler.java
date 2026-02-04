package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.tarzan.maxkb4j.core.workflow.annotation.NodeHandlerType;
import com.tarzan.maxkb4j.core.workflow.builder.NodeBuilder;
import com.tarzan.maxkb4j.core.workflow.enums.NodeType;
import com.tarzan.maxkb4j.core.workflow.handler.WorkflowHandler;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.logic.LogicFlow;
import com.tarzan.maxkb4j.core.workflow.model.LoopWorkFlow;
import com.tarzan.maxkb4j.core.workflow.model.NodeResult;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.AbsNode;
import com.tarzan.maxkb4j.core.workflow.node.impl.LoopNode;
import com.tarzan.maxkb4j.module.application.domain.vo.ChatMessageVO;
import com.tarzan.maxkb4j.module.chat.dto.ChatParams;
import com.tarzan.maxkb4j.module.chat.dto.ChildNode;
import com.tarzan.maxkb4j.module.chat.dto.LoopParams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.*;

@NodeHandlerType(NodeType.LOOP)
@Component
@RequiredArgsConstructor
public class LoopNodeHandler implements INodeHandler {

    private final WorkflowHandler workflowHandler;


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
                if (value instanceof List<?>) {
                    List<Object> list = (List<Object>) value;
                    loopDetails = generateLoopArray(list, workflow, loopBody, node);
                } else {
                    Gson gson = new Gson();
                    String inputStr = value.toString().trim();
                    if (!inputStr.startsWith("[") && !inputStr.endsWith("]")) {
                        inputStr = "[" + inputStr + "]";
                    }
                    List<Object> resultList = gson.fromJson(inputStr, new TypeToken<List<Object>>() {
                    }.getType());
                    loopDetails = generateLoopArray(resultList, workflow, loopBody, node);
                }
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

    public boolean isInterrupt(AbsNode node) {
        return node.getDetail().containsKey("is_interrupt_exec") && (boolean) node.getDetail().get("is_interrupt_exec");
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
            Sinks.Many<ChatMessageVO> nodeSink = Sinks.many().unicast().onBackpressureBuffer();
            LogicFlow logicFlow = LogicFlow.newInstance(loopBody);
            List<AbsNode> nodes = logicFlow.getNodes().stream().map(NodeBuilder::getNode).filter(Objects::nonNull).toList();
            LoopParams loopParams = new LoopParams(startIndex, list.get(startIndex));
            LoopWorkFlow loopWorkflow = new LoopWorkFlow(
                    workflow,
                    nodes,
                    logicFlow.getEdges(),
                    loopParams,
                    details,
                    nodeSink);
            // 异步执行
            CompletableFuture<String> future = workflowHandler.executeAsync(loopWorkflow);
            AtomicReference<ChildNode> childNode = new AtomicReference<>(null);
            // 订阅并累积 token，同时发送消息
            nodeSink.asFlux().subscribe(e -> {
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

                    // 使用策略来决定是否发送到主工作流的sink
                    if (workflow.needsSinkOutput()) {
                        workflow.getSink().tryEmitNext(vo);
                    }
                }
            });
            ChatMessageVO vo = node.toChatMessageVO(
                    workflow.getChatParams().getChatId(),
                    workflow.getChatParams().getChatRecordId(),
                    "",
                    "",
                    childNode.get(),
                    false);

            // 同样使用策略来决定是否发送消息结束标记
            if (workflow.needsSinkOutput()) {
                workflow.getSink().tryEmitNext(vo);
            }
            future.join();
            node.getDetail().put("is_interrupt_exec", isInterruptExec.get());
            node.getDetail().put("current_index", startIndex);
            JSONObject runtimeDetails = loopWorkflow.getRuntimeDetails();
            for (String key : runtimeDetails.keySet()) {
                JSONObject value = runtimeDetails.getJSONObject(key);
                String runtimeNodeId = value.getString("runtimeNodeId");
                runtimeNodeId=runtimeNodeId+"_"+startIndex;
                value.put("runtimeNodeId", runtimeNodeId);
            }
            loopDetails.add(runtimeDetails);
            if (breakOuter.get()) {
                break;
            }
            chatParams.setRuntimeNodeId(null);
            startIndex++;
        } while (startIndex < list.size());
        return loopDetails;
    }


}
