package com.tarzan.maxkb4j.core.workflow.handler.node;

import com.tarzan.maxkb4j.core.workflow.node.AbsNode;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.model.NodeResult;

public interface INodeHandler {

    NodeResult execute(Workflow workflow, AbsNode node) throws Exception;

}
