package com.tarzan.maxkb4j.core.workflow.handler;

import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.INode;
import com.tarzan.maxkb4j.core.workflow.result.NodeResult;
import com.tarzan.maxkb4j.core.workflow.result.NodeResultFuture;
import com.tarzan.maxkb4j.module.application.domian.vo.ChatMessageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowHandler {


    public String execute(Workflow workflow) {
        INode currentNode = workflow.getCurrentNode();
        if (currentNode == null) {
            currentNode = workflow.getStartNode();
        }
        // 运行节点并获取下一个节点列表
        List<INode> nodeList = runChainNode(workflow, currentNode);
        runChainNodes(workflow, nodeList);
        ChatMessageVO vo = new ChatMessageVO(workflow.getChatParams().getChatId(), workflow.getChatParams().getChatRecordId(), true);
        workflow.getChatParams().getSink().tryEmitNext(vo);
        return workflow.getAnswer();
    }

    public void runChainNodes(Workflow workflow, List<INode> nodeList) {
        for (INode node : nodeList) {
            List<INode> nextNodeList = runChainNode(workflow, node);
            runChainNodes(workflow, nextNodeList);
        }
    }

    public void parallelRunChainNodes(Workflow workflow, List<INode> nodeList) {
        if (nodeList == null || nodeList.isEmpty()) {
            return;
        }
        List<CompletableFuture<List<INode>>> futureList = new ArrayList<>();
        for (INode node : nodeList) {
            futureList.add(CompletableFuture.supplyAsync(() -> runChainNode(workflow, node)));
        }
        List<INode> nextNodeList = futureList.stream().flatMap(future -> future.join().stream()).toList();
        Set<String> seen = new HashSet<>();
        List<INode> uniqueList = nextNodeList.stream()
                .filter(e -> seen.add(e.getId()))
                .toList();
        runChainNodes(workflow, uniqueList);
    }

    public List<INode> runChainNode(Workflow workflow, INode node) {
        // 处理默认的nodeResultFuture
        NodeResultFuture nodeResultFuture = runNodeFuture(workflow, node);
        NodeResult nodeResult = nodeResultFuture.getResult();
        if (nodeResult != null) {
            nodeResult.writeContext(node, workflow);
        }
        // 添加已运行节点
        workflow.appendNode(node);
        // 获取下一个节点列表
        return workflow.getNextNodeList(node, nodeResult);
    }

    public NodeResultFuture runNodeFuture(Workflow workflow, INode node) {
        try {
            long startTime = System.currentTimeMillis();
            INodeHandler nodeHandler = NodeHandlerBuilder.getHandler(node.getType());
            NodeResult result = nodeHandler.execute(workflow, node);
            float runTime = (System.currentTimeMillis() - startTime) / 1000F;
            node.getDetail().put("runTime", runTime);
            log.info("node:{}, runTime:{} s", node.getProperties().getString("nodeName"), runTime);
            return new NodeResultFuture(result, null, 200);
        } catch (Exception ex) {
            log.error("error:", ex);
            node.setStatus(500);
            node.setErrMessage(ex.getMessage());
            workflow.getChatParams().getSink().tryEmitError(ex);
            log.error("NODE: {} ERROR :{}", node.getType(), ex.getCause().getMessage());
            return new NodeResultFuture(null, ex, 500);
        }
    }


}
