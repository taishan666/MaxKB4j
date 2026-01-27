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

    public String execute(Workflow workflow) {
        AbsNode currentNode = workflow.getCurrentNode();
        if (currentNode == null) {
            currentNode = workflow.getStartNode();
        }
        runChainNodes(workflow, List.of(currentNode));
        return workflow.getAnswer();
    }

    public CompletableFuture<String> executeAsync(Workflow workflow) {
        return CompletableFuture.completedFuture(execute(workflow));
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
        if (NodeStatus.READY.getCode()==node.getStatus()||NodeStatus.INTERRUPT.getCode()==node.getStatus()) {
            if (workflow.dependentNodeBeenExecuted(node)){
                workflow.appendNode(node);
                NodeResultFuture nodeResultFuture = runNodeFuture(workflow, node);
                node.setStatus(nodeResultFuture.getStatus());
                NodeResult nodeResult = nodeResultFuture.getResult();
                if (nodeResult != null) {
                    nodeResult.writeContext(node, workflow);
                    nodeResult.writeDetail(node);
                    if(nodeResult.isInterruptExec(node)){
                        node.setStatus(NodeStatus.INTERRUPT.getCode());
                    }
                }
                // 获取下一个节点列表
                return workflow.getNextNodeList(node, nodeResult);
            }
        }else if (NodeStatus.SKIP.getCode()==node.getStatus()) {
            // 获取下一个节点列表
            List<AbsNode> nextNodeList = workflow.getNextNodeList(node, new NodeResult(Map.of()));
            nextNodeList.forEach(nextNode -> {
                if (!workflow.isReadyJoinNode(nextNode)){
                    nextNode.setStatus(NodeStatus.SKIP.getCode());
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
            return new NodeResultFuture(result, null, NodeStatus.SUCCESS.getCode());
        } catch (Exception ex) {
            log.error("error:", ex);
            node.setErrMessage(ex.getMessage());
            log.error("NODE: {} Exception :{}", node.getType(), ex.getMessage());
            // 添加空指针检查，防止 chatParams 为 null 时导致的异常
            if (workflow.getChatParams() != null && workflow.getSink() != null) {
                ChatMessageVO errMessage = node.toChatMessageVO(
                        workflow.getChatParams().getChatId(),
                        workflow.getChatParams().getChatRecordId(),
                        String.format("Exception: %s", ex.getMessage()),
                        "",
                        null,
                        true);
                workflow.getSink().tryEmitNext(errMessage);
            }
            return new NodeResultFuture(null, ex, NodeStatus.ERROR.getCode());
        }
    }


}



