package com.tarzan.maxkb4j.core.workflow.model;

import com.tarzan.maxkb4j.core.workflow.enums.NodeType;
import com.tarzan.maxkb4j.core.workflow.logic.LfEdge;
import com.tarzan.maxkb4j.core.workflow.node.AbsNode;
import com.tarzan.maxkb4j.module.application.domain.vo.ChatMessageVO;
import com.tarzan.maxkb4j.module.chat.dto.ChatParams;
import com.tarzan.maxkb4j.module.chat.dto.LoopParams;
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

    public LoopWorkFlow(List<AbsNode> nodes, List<LfEdge> edges, ChatParams chatParams, LoopParams loopParams,Sinks.Many<ChatMessageVO> sink) {
        super(nodes, edges, chatParams, sink);
        this.loopParams = loopParams;
        this.loopContext = new HashMap<>();
    }

    @Override
    public AbsNode getStartNode() {
        return getNodeClsById(NodeType.LOOP_START.getKey(), List.of(), null);
    }
}
