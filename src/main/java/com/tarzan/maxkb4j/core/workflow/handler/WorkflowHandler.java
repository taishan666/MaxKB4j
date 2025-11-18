package com.tarzan.maxkb4j.core.workflow.handler;

import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.node.INode;
import com.tarzan.maxkb4j.core.workflow.result.NodeResult;
import com.tarzan.maxkb4j.core.workflow.result.NodeResultFuture;
import com.tarzan.maxkb4j.module.application.domian.vo.ChatMessageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class WorkflowHandler {


    public String execute(Workflow workflow) {
        runChainManage(workflow, workflow.getCurrentNode());
        ChatMessageVO vo = new ChatMessageVO(workflow.getChatParams().getChatId(), workflow.getChatParams().getChatRecordId(), true);
        workflow.getChatParams().getSink().tryEmitNext(vo);
        return workflow.getAnswer();
    }


    public void runChainManage(Workflow workflow, INode currentNode) {
        if (currentNode == null) {
            currentNode = workflow.getStartNode();
        }
        NodeResult result = runChainNode(workflow, currentNode);
        // 获取下一个节点列表
        List<INode> nodeList = workflow.getNextNodeList(currentNode, result);
        if (nodeList.size() == 1) {
            runChainManage(workflow, nodeList.get(0));
        } else if (nodeList.size() > 1) {
            // 提交子任务并获取Future对象
            for (INode node : nodeList) {
                runChainManage(workflow, node);
            }
        }
    }

    public NodeResult runChainNode(Workflow workflow, INode currentNode) {
        // 添加节点
        workflow.appendNode(currentNode);
        // 处理默认的nodeResultFuture
        NodeResultFuture nodeResultFuture = runNodeFuture(workflow,currentNode);
        NodeResult currentResult = nodeResultFuture.getResult();
        if (currentResult != null) {
            currentResult.writeContext(currentNode, workflow);
        }
        return currentResult;
    }

    public NodeResultFuture runNodeFuture(Workflow workflow,INode node) {
        try {
            long startTime = System.currentTimeMillis();
            INodeHandler nodeHandler = NodeHandlerBuilder.getHandler(node.getType());
            NodeResult result = nodeHandler.execute(workflow,node);
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
