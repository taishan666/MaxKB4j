package com.tarzan.maxkb4j.core.workflow.handler;

import com.tarzan.maxkb4j.core.workflow.builder.NodeHandlerBuilder;
import com.tarzan.maxkb4j.core.workflow.enums.NodeStatus;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.model.NodeResult;
import com.tarzan.maxkb4j.core.workflow.model.NodeResultFuture;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.AbsNode;
import com.tarzan.maxkb4j.module.application.domain.vo.ChatMessageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowHandler {

    public void execute(Workflow workflow) {
        AbsNode currentNode = workflow.getCurrentNode();
        if (currentNode == null) {
            currentNode = workflow.getStartNode();
        }
        log.info("工作流开始-开始节点:{}", currentNode.getType());
        runChainNodes(workflow, List.of(currentNode));
        log.info("工作流结束");
    }


    public CompletableFuture<Void> executeAsync(Workflow workflow) {
        return CompletableFuture.runAsync(() -> {
            execute(workflow);
        });
    }



    public void runChainNodes(Workflow workflow, List<AbsNode> nodeList) {
        if (nodeList == null || nodeList.isEmpty()) {
            return;
        }
        if(nodeList.size()==1){
            List<AbsNode>  nextNodeList = runChainNode(workflow, nodeList.get(0));
            runChainNodes(workflow, nextNodeList);
        }else {
            List<CompletableFuture<List<AbsNode>>> futureList = new ArrayList<>();
            for (AbsNode node : nodeList) {
                futureList.add(CompletableFuture.supplyAsync(() -> runChainNode(workflow, node)));
            }
            List<List<AbsNode>> nextNodeLists = futureList.stream()
                    .map(CompletableFuture::join)
                    .toList();
            for (List<AbsNode> nextNodeList : nextNodeLists) {
                runChainNodes(workflow, nextNodeList);
            }
        }
    }


    public List<AbsNode> runChainNode(Workflow workflow, AbsNode node) {
        if (NodeStatus.READY.getStatus()==node.getStatus()||NodeStatus.INTERRUPT.getStatus()==node.getStatus()) {
            if (workflow.dependentNodeBeenExecuted(node)){
                NodeResultFuture nodeResultFuture = runNodeFuture(workflow, node);
                node.setStatus(nodeResultFuture.getStatus());
                NodeResult nodeResult = nodeResultFuture.getResult();
                if (nodeResult != null) {
                    nodeResult.writeContext(node, workflow);
                    nodeResult.writeDetail(node);
                    if(nodeResult.isInterruptExec(node)){
                        node.setStatus(NodeStatus.INTERRUPT.getStatus());
                    }
                }
                // 获取下一个节点列表
                return workflow.getNextNodeList(node, nodeResult);
            }
        }else if (NodeStatus.SKIP.getStatus()==node.getStatus()) {
            // 获取下一个节点列表
            List<AbsNode> nextNodeList = workflow.getNextNodeList(node, new NodeResult(Map.of()));
            nextNodeList.forEach(nextNode -> {
                if (!workflow.isReadyJoinNode(nextNode)){
                    nextNode.setStatus(NodeStatus.SKIP.getStatus());
                }
            });
            return nextNodeList;
        }
        return List.of();
    }

    public NodeResultFuture runNodeFuture(Workflow workflow, AbsNode node) {
        try {
            long startTime = System.currentTimeMillis();
            INodeHandler nodeHandler = NodeHandlerBuilder.getHandler(node.getType());
            NodeResult result = nodeHandler.execute(workflow, node);
            float runTime = (System.currentTimeMillis() - startTime) / 1000F;
            node.getDetail().put("runTime", runTime);
            log.info("node:{}, runTime:{} s", node.getProperties().getString("nodeName"), runTime);
            return new NodeResultFuture(result, null, NodeStatus.SUCCESS.getStatus());
        } catch (Exception ex) {
            log.error("error:", ex);
            node.setErrMessage(ex.getMessage());
            log.error("NODE: {} Exception :{}", node.getType(), ex.getMessage());
            // 使用工作流的sink输出决策
            if (workflow.getChatParams() != null && workflow.needsSinkOutput()) {
                ChatMessageVO errMessage = node.toChatMessageVO(
                        workflow.getChatParams().getChatId(),
                        workflow.getChatParams().getChatRecordId(),
                        String.format("Exception: %s", ex.getMessage()),
                        "",
                        null,
                        true);
                workflow.getSink().tryEmitNext(errMessage);
            }
            return new NodeResultFuture(null, ex, NodeStatus.ERROR.getStatus());
        }
    }


}



