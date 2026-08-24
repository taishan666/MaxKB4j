package com.maxkb4j.workflow.handler.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.handler.node.AbsNodeHandler;
import com.maxkb4j.workflow.handler.node.loop.LoopIterationRunner;
import com.maxkb4j.workflow.handler.node.loop.LoopMessageForwarder;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.LoopNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.maxkb4j.workflow.enums.NodeType.LOOP;
import static com.maxkb4j.workflow.consts.WorkflowConstants.LoopField;

/**
 * 循环节点处理器
 * 支持数组遍历、指定次数循环和无限循环三种模式。
 *
 * <p>职责已收敛为节点执行编排：迭代控制委托给 {@link LoopIterationRunner}，
 * 消息输出（订阅/转发/迭代标记）委托给 {@link LoopMessageForwarder}，
 * 子工作流构建统一经 {@link com.maxkb4j.workflow.service.WorkflowFactory}。</p>
 */
@NodeHandlerType(LOOP)
@Component
@RequiredArgsConstructor
public class LoopNodeHandler extends AbsNodeHandler {

    // Detail 键常量
    private static final String DETAIL_LOOP_DATA = LoopField.LOOP_NODE_DATA;
    private static final String DETAIL_LOOP_TYPE = LoopField.LOOP_TYPE;
    private static final String DETAIL_NUMBER = LoopField.NUMBER;

    private final LoopIterationRunner iterationRunner;

    @Override
    public NodeResult doExecute(IWorkflow workflow, AbsNode node) throws Exception {
        LoopNode.NodeParams nodeParams = parseParams(node, LoopNode.NodeParams.class);
        List<JSONObject> loopDetails = iterationRunner.run(workflow, node, nodeParams);
        node.getDetail().put(DETAIL_LOOP_DATA, loopDetails);
        node.getDetail().put(DETAIL_LOOP_TYPE, nodeParams.getLoopType());
        node.getDetail().put(DETAIL_NUMBER, nodeParams.getNumber());
        return new NodeResult(workflow.getLoopContext(), true, this::getInterruptFlag);
    }
}
