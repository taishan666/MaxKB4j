package com.maxkb4j.core.workflow.handler.node;


import com.maxkb4j.core.workflow.model.NodeResult;
import com.maxkb4j.core.workflow.model.Workflow;
import com.maxkb4j.core.workflow.node.AbsNode;

public interface INodeHandler {

    NodeResult execute(Workflow workflow, AbsNode node) throws Exception;

}
