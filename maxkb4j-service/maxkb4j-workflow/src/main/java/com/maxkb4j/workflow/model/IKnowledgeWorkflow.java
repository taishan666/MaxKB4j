package com.maxkb4j.workflow.model;

/**
 * 知识库系工作流契约
 * 知识库工作流（含知识库循环工作流）在完整工作流能力（{@link IWorkflow}）之上，
 * 额外提供知识库参数访问。
 */
public interface IKnowledgeWorkflow extends IWorkflow {

    /**
     * 获取知识库参数
     */
    KnowledgeParams getKnowledgeParams();

}
