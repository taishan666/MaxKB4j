package com.tarzan.maxkb4j.core.workflow.handler.node;

import com.tarzan.maxkb4j.core.workflow.node.INode;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.result.NodeResult;

public interface INodeHandler {

    NodeResult execute(Workflow workflow, INode node) throws Exception;

}
