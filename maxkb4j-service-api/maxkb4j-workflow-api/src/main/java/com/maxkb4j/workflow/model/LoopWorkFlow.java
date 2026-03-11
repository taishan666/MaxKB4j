package com.maxkb4j.workflow.model;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.core.chat.ChatMessageVO;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.logic.LfEdge;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.Getter;
import lombok.Setter;
import reactor.core.publisher.Sinks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class LoopWorkFlow extends Workflow {

    private LoopParams loopParams;
    private Map<String, Object> loopContext;

    public LoopWorkFlow(Workflow workflow, List<AbsNode> nodes, List<LfEdge> edges, LoopParams loopParams, JSONObject details, Sinks.Many<ChatMessageVO> sink) {
        super(workflow.getWorkflowMode(), nodes, edges, workflow.getChatParams(), details, sink);
        this.loopParams = loopParams;
        this.loopContext = new HashMap<>();
        this.setWorkflowContext(workflow.getWorkflowContext());
        this.setVariableResolver(new VariableResolver(this.getWorkflowContext(), this.loopContext));
        this.setTemplateRenderer(new TemplateRenderer(this.getVariableResolver()));
    }


    @Override
    public AbsNode getStartNode() {
        return getNodeClsById(NodeType.LOOP_START.getKey(), List.of(), null);
    }


}
