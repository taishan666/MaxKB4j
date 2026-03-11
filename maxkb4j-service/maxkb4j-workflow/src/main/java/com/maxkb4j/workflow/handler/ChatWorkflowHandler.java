package com.maxkb4j.workflow.handler;

import com.maxkb4j.common.domain.dto.ChatMessageVO;
import com.maxkb4j.workflow.builder.NodeHandlerBuilder;
import com.maxkb4j.workflow.enums.NodeStatus;
import com.maxkb4j.workflow.handler.node.INodeHandler;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.NodeResultFuture;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWorkflowHandler implements IWorkflowHandler {

    public void execute(Workflow workflow) {
        AbsNode currentNode = workflow.getCurrentNode();
        if (currentNode == null) {
            currentNode = workflow.getStartNode();
        }
        log.info("工作流-开始");
        runChainNodes(workflow, List.of(currentNode));
        log.info("工作流-结束");
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



