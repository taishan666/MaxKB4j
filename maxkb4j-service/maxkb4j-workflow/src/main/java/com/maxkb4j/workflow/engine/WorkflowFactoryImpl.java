package com.maxkb4j.workflow.engine;

import com.maxkb4j.common.domain.dto.ChatState;
import com.maxkb4j.common.domain.dto.ChatMessageVO;
import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.workflow.enums.WorkflowMode;
import com.maxkb4j.workflow.logic.LfEdge;
import com.maxkb4j.workflow.model.KnowledgeParams;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.service.WorkflowFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

import java.util.List;

/**
 * 工作流工厂实现
 * 供外部模块（application / knowledge）构造工作流，隔离引擎实现细节。
 */
@Component
public class WorkflowFactoryImpl implements WorkflowFactory {

    @Override
    public IWorkflow createApplication(List<AbsNode> nodes, List<LfEdge> edges, ChatParams chatParams, ChatState chatContext, Sinks.Many<ChatMessageVO> sink) {
        return WorkflowBuilder.create(WorkflowMode.APPLICATION, nodes, edges)
                .chatParams(chatParams)
                .context(chatContext)
                .sink(sink)
                .build();
    }

    @Override
    public IWorkflow createKnowledge(List<AbsNode> nodes, List<LfEdge> edges, KnowledgeParams params) {
        return new KnowledgeWorkflow(nodes, edges, params);
    }
}
