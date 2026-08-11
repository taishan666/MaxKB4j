package com.maxkb4j.workflow.engine.graph;

import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.common.domain.dto.ChatState;
import com.maxkb4j.workflow.enums.WorkflowMode;
import com.maxkb4j.workflow.model.IChatWorkflow;
import com.maxkb4j.workflow.model.IKnowledgeWorkflow;
import com.maxkb4j.workflow.model.KnowledgeParams;
import com.maxkb4j.workflow.model.LoopParams;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 回归测试：循环工作流同时持有父工作流参数与自身循环参数。
 */
class LoopWorkflowTest {

    @Test
    void chatLoopWorkflow_exposesChatParamsChatStateAndLoopParams() {
        ChatParams chatParams = ChatParams.builder().message("hi").chatId("c1").build();
        ChatState chatState = ChatState.builder().build();
        ChatWorkflow parent = ChatWorkflowBuilder.create(WorkflowMode.APPLICATION, List.of(), List.of())
                .chatParams(chatParams)
                .chatState(chatState)
                .build();
        LoopParams loopParams = new LoopParams(0, "item-0");

        ChatLoopWorkflow loopWorkflow = new ChatLoopWorkflow(parent, List.of(), List.of(), loopParams, null);

        assertThat(loopWorkflow.getChatParams()).isSameAs(chatParams);
        assertThat(loopWorkflow.getChatState()).isSameAs(chatState);
        assertThat(loopWorkflow.getLoopParams()).isSameAs(loopParams);
        assertThat(loopWorkflow).isInstanceOf(IChatWorkflow.class);
    }

    @Test
    void knowledgeLoopWorkflow_exposesKnowledgeParamsAndLoopParams() {
        KnowledgeParams knowledgeParams = KnowledgeParams.builder().knowledgeId("k1").build();
        KnowledgeWorkflow parent = new KnowledgeWorkflow(List.of(), List.of(), knowledgeParams);
        LoopParams loopParams = new LoopParams(1, "item-1");

        KnowledgeLoopWorkflow loopWorkflow = new KnowledgeLoopWorkflow(parent, List.of(), List.of(), loopParams);

        assertThat(loopWorkflow.getKnowledgeParams()).isSameAs(knowledgeParams);
        assertThat(loopWorkflow.getLoopParams()).isSameAs(loopParams);
        assertThat(loopWorkflow).isInstanceOf(IKnowledgeWorkflow.class);
    }
}
