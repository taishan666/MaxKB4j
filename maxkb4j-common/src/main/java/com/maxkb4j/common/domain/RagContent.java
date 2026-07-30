package com.maxkb4j.common.domain;

/**
 * RAG 注入内容的最小契约。
 *
 * <p>RagContentInjector 仅依赖标题与正文两个属性完成 prompt 拼装，
 * 通过该接口与具体业务 VO（如 knowledge 的 {@code ParagraphVO}）解耦，
 * 使 core 无需依赖任何业务 api 模块。
 */
public interface RagContent {

    /**
     * 内容标题，可为空。
     */
    String getTitle();

    /**
     * 内容正文。
     */
    String getContent();
    /**
     * 文档名称。
     */
    String getDocumentName();
}
