package com.maxkb4j.workflow.handler.node;


import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;

public interface INodeHandler {

    NodeResult execute(Workflow workflow, AbsNode node) throws Exception;

}
