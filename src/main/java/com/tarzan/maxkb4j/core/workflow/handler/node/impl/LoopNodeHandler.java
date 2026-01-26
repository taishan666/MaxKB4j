package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import com.alibaba.fastjson.JSONObject;
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
import com.tarzan.maxkb4j.module.chat.dto.ChildNode;
import com.tarzan.maxkb4j.module.chat.dto.LoopParams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.FORM;
import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.USER_SELECT;

@NodeHandlerType(NodeType.LOOP)
@Component
@RequiredArgsConstructor
public class LoopNodeHandler implements INodeHandler {

    private final WorkflowHandler workflowHandler;

    @Override
    public NodeResult execute(Workflow workflow, AbsNode node) throws Exception {
        LoopNode.NodeParams nodeParams = node.getNodeData().toJavaObject(LoopNode.NodeParams.class);
        String loopType = nodeParams.getLoopType();
        List<String> array = nodeParams.getArray();
        Integer number = nodeParams.getNumber();
        JSONObject loopBody = nodeParams.getLoopBody();
        if ("ARRAY".equals(loopType)) {
            generateLoopArray(array,workflow, loopBody, node);
        } else if ("LOOP".equals(loopType)) {
            generateWhileLoop(workflow, loopBody,node);
        } else {
            generateLoopNumber(number,workflow, loopBody,node);
        }
        return  new NodeResult(Map.of());
    }


    private void loop(Workflow workflow, JSONObject loopBody,LoopParams loopParams, AbsNode node) {
        LogicFlow logicFlow = LogicFlow.newInstance(loopBody);
        List<AbsNode> nodes = logicFlow.getNodes().stream().map(NodeBuilder::getNode).filter(Objects::nonNull).toList();
        Sinks.Many<ChatMessageVO> nodeSink = Sinks.many().unicast().onBackpressureBuffer();
        LoopWorkFlow  loopWorkflow= new LoopWorkFlow(
                nodes,
                logicFlow.getEdges(),
                workflow.getChatParams(),
                loopParams,
                nodeSink);
       // 异步执行
        CompletableFuture<String> future = workflowHandler.executeAsync(loopWorkflow);
        // 使用原子变量或收集器来安全地累积 token
        AtomicBoolean isInterruptExec=new AtomicBoolean( false);
            // 订阅并累积 token，同时发送消息
        nodeSink.asFlux().subscribe(e -> {
                if(FORM.getKey().equals(e.getNodeType())||USER_SELECT.getKey().equals(e.getNodeType())){
                    isInterruptExec.set(true);
                }
                ChildNode childNode=new ChildNode(e.getChatRecordId(),e.getRuntimeNodeId());
                ChatMessageVO vo = node.toChatMessageVO(
                        loopParams.getIndex(),
                        workflow.getChatParams().getChatId(),
                        workflow.getChatParams().getChatRecordId(),
                        e.getContent(),
                        e.getReasoningContent(),
                        childNode,
                        false);
                if (workflow.getSink() != null) {
                    workflow.getSink().tryEmitNext(vo);
                }
        });
        future.join();
        JSONObject runtimeDetails = loopWorkflow.getRuntimeDetails();
        node.getDetail().put("is_interrupt_exec", isInterruptExec.get());
    }

    private void generateLoopArray(List<String> array, Workflow workflow,JSONObject loopBody, AbsNode node) {
        for (int i = 0; i < array.size(); i++) {
            Object item = array.get(i);
            loop(workflow,loopBody,new LoopParams(i,item), node);
        }
    }

    private void generateLoopNumber(Integer number, Workflow workflow,  JSONObject loopBody, AbsNode node) {
        for (int i = 0; i < number; i++) {
            loop(workflow,loopBody,new LoopParams(i,i), node);
        }
    }

    private void generateWhileLoop(Workflow workflow,  JSONObject loopBody,AbsNode node) {
        int i = 0;
        do {
            loop(workflow,loopBody,new LoopParams(i,i), node);
            i++;
        } while (i <= 500);
    }

}
